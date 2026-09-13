package com.geoviksoft.turnia.core.data.datasource.firestore

import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackData
import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackedSnapshots
import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackWrite
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.DocHolder
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.GroupDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.errors.GenericFirestoreError
import com.geoviksoft.turnia.core.data.datasource.firestore.sync.SharedListeners
import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.outcomeCatching
import com.geoviksoft.turnia.core.system.toInstantOrNull
import com.geoviksoft.turnia.core.system.valueOrNull
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlin.time.Instant

/**
 * Interacts with Firestore: `groups/{groupId}`
 */
class GroupFirestore(
    private val firestore: FirebaseFirestore,
    private val groupSyncFirestore: GroupSyncFirestore,
    scope: CoroutineScope,
    ) {

    private val userGroupsFlow = SharedListeners<UserId, List<DocHolder<GroupDocument>>>(scope)

    fun observe(groupId: GroupId): Flow<DocHolder<GroupDocument>?> = flow {
        var known = cachedGroup(groupId)
        emit(known)

        var fetched = known != null
        emitAll(
            groupSyncFirestore.observe(groupId).mapNotNull { sync ->
                if (fetched && !isStale(known, sync.groupUpdatedAt.toInstantOrNull())) {
                    return@mapNotNull null
                }

                known = serverGroup(groupId)
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
            val cached = cachedGroup(groupId)

            // Nothing cached: asking the sync document first would only add a read to a fetch that
            // is going to happen anyway.
            if (cached == null) return@outcomeCatching serverGroup(groupId)

            val serverUpdatedAt = groupSyncFirestore.get(groupId).valueOrNull()
                ?.groupUpdatedAt.toInstantOrNull()

            if (isStale(cached, serverUpdatedAt)) serverGroup(groupId) else cached
        }

    private fun isStale(cached: DocHolder<GroupDocument>?, serverUpdatedAt: Instant?): Boolean {
        if (serverUpdatedAt == null) return false
        val cacheUpdatedAt = cached?.doc?.updateAt.toInstantOrNull() ?: return true

        return serverUpdatedAt > cacheUpdatedAt
    }

    fun observeMyGroups(userId: UserId): Flow<List<DocHolder<GroupDocument>>> =
        userGroupsFlow.shared(userId) { snapshotMyGroups(userId) }

    private fun snapshotMyGroups(userId: UserId): Flow<List<DocHolder<GroupDocument>>> =
        firestore.collection(PATH_GROUPS)
            .where { GroupDocument.FIELD_MEMBER_UIDS contains userId.value }
            .trackedSnapshots(TAG, "myGroups(snapshots)")
            .map { snapshot ->
                Logger.d(TAG, "Groups of the user: ${snapshot.documents.size}")

                snapshot.documents.map {
                    DocHolder(id = it.reference.id, doc = it.data(GroupDocument.serializer()))
                }
            }
            // Metadata changes arrive too, and repeat the list unchanged.
            .distinctUntilChanged()
            .catch { throwable ->
                Logger.e(TAG, "Groups listener failed", throwable)
                emit(emptyList())
            }

    suspend fun create(groupId: GroupId, group: GroupDocument): Outcome<Unit, GenericFirestoreError> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) {
            Logger.d(TAG, "Create group document")

            groupDocument(groupId).set(group)
            trackWrite(TAG, "createGroup")
        }

    suspend fun update(groupId: GroupId, group: GroupDocument): Outcome<Unit, GenericFirestoreError> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) {
            Logger.d(TAG, "Set group document")

            // The group and its marker in one commit, so both resolve to the same instant and a
            // reader can see them as equally old.
            val batch = firestore.batch()
            batch.set(groupDocument(groupId), group)
            val syncWrite = groupSyncFirestore.writeGroup(batch, groupId)
            batch.commit()
            trackWrite(TAG, "updateGroup")
            syncWrite.committed()
        }

    private suspend fun cachedGroup(groupId: GroupId): DocHolder<GroupDocument>? =
        groupDocument(groupId).getCached(TAG, "groupDoc(CACHE)")?.let { snapshot ->
            Logger.d(TAG, "Group document from the cache")
            DocHolder(id = snapshot.reference.id, doc = snapshot.data(GroupDocument.serializer()))
        }

    private suspend fun serverGroup(groupId: GroupId): DocHolder<GroupDocument>? {
        val snapshot = groupDocument(groupId).get(Source.SERVER).trackData(TAG, "groupDoc(SERVER)")
        Logger.d(TAG, "Group document from the server. Exists: ${snapshot.exists}")

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
