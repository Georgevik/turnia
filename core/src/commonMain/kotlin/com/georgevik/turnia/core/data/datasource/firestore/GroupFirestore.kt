package com.georgevik.turnia.core.data.datasource.firestore

import com.georgevik.turnia.core.data.datasource.firestore.analytics.trackData
import com.georgevik.turnia.core.data.datasource.firestore.analytics.trackWrite
import com.georgevik.turnia.core.data.datasource.firestore.doc.DocHolder
import com.georgevik.turnia.core.data.datasource.firestore.doc.GroupDocument
import com.georgevik.turnia.core.data.datasource.firestore.errors.GenericFirestoreError
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.GroupId
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.outcomeCatching
import com.georgevik.turnia.core.system.toInstantOrNull
import com.georgevik.turnia.core.system.valueOrNull
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlin.time.Instant

/**
 * Interacts with Firestore: `groups/{groupId}`
 */
class GroupFirestore(
    private val firestore: FirebaseFirestore,
    private val groupSyncFirestore: GroupSyncFirestore,
) {

    fun observe(groupId: GroupId): Flow<DocHolder<GroupDocument>?> = flow {
        var known = queryGroup(groupId, Source.CACHE)
        emit(known)

        var fetched = known != null
        emitAll(
            groupSyncFirestore.observe(groupId).mapNotNull { sync ->
                if (fetched && !isStale(known, sync.groupUpdatedAt.toInstantOrNull())) {
                    return@mapNotNull null
                }

                known = queryGroup(groupId, Source.SERVER)
                fetched = true
                known
            }
        )
    }.catch { throwable ->
        Logger.e(TAG, "Group listener failed", throwable)
        emit(null)
    }

    /** The best answer available now, for callers with nothing to keep up to date. */
    suspend fun get(groupId: GroupId): Outcome<DocHolder<GroupDocument>?, GenericFirestoreError> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) {
            val cached = queryGroup(groupId, Source.CACHE)

            // Nothing cached: asking the sync document first would only add a read to a fetch that
            // is going to happen anyway.
            if (cached == null) return@outcomeCatching queryGroup(groupId, Source.SERVER)

            val serverUpdatedAt = groupSyncFirestore.get(groupId).valueOrNull()
                ?.groupUpdatedAt.toInstantOrNull()

            if (isStale(cached, serverUpdatedAt)) queryGroup(groupId, Source.SERVER) else cached
        }

    private fun isStale(cached: DocHolder<GroupDocument>?, serverUpdatedAt: Instant?): Boolean {
        if (serverUpdatedAt == null) return false
        val cacheUpdatedAt = cached?.doc?.updateAt.toInstantOrNull() ?: return true

        return serverUpdatedAt > cacheUpdatedAt
    }

    suspend fun getMyGroups(
        userId: UserId
    ): Outcome<List<DocHolder<GroupDocument>>, GenericFirestoreError> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) {
            val snapshot = firestore.collection(PATH_GROUPS).where {
                GroupDocument.FIELD_MEMBER_UIDS contains userId.value
            }.get().trackData(TAG)
            Logger.d(TAG, "Groups of the user: ${snapshot.documents.size}")

            snapshot.documents.map {
                DocHolder(id = it.reference.id, doc = it.data(GroupDocument.serializer()))
            }
        }

    suspend fun save(groupId: GroupId, group: GroupDocument): Outcome<Unit, GenericFirestoreError> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) {
            Logger.d(TAG, "Set group document")

            // The group and its marker in one commit, so both resolve to the same instant and a
            // reader can see them as equally old.
            val batch = firestore.batch()
            batch.set(groupDocument(groupId), group)
            groupSyncFirestore.writeGroup(batch, groupId)
            batch.commit()
            trackWrite(TAG)
        }

    private suspend fun queryGroup(groupId: GroupId, source: Source): DocHolder<GroupDocument>? {
        val snapshot = groupDocument(groupId).get(source).trackData(TAG)
        Logger.d(TAG, "Group document. Source: $source. Exists: ${snapshot.exists}")

        if (!snapshot.exists) return null
        return DocHolder(
            id = snapshot.reference.id,
            doc = snapshot.data(GroupDocument.serializer()),
        )
    }

    private fun groupDocument(groupId: GroupId) =
        firestore.collection(PATH_GROUPS).document(groupId.value)

    companion object {
        private const val TAG = "GroupFirestore"
        private const val PATH_GROUPS = "groups"
    }
}
