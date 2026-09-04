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

/**
 * Interacts with Firestore: `groups/{groupId}`
 */
class GroupFirestore(
    private val firestore: FirebaseFirestore,
    private val groupSyncFirestore: GroupSyncFirestore,
) {

    suspend fun get(groupId: GroupId): Outcome<DocHolder<GroupDocument>?, GenericFirestoreError> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) {
            val cached = read(groupId, Source.CACHE)
            val cacheUpdatedAt = cached?.doc?.updateAt.toInstantOrNull()

            // Nothing cached: asking the sync document first would only add a read to a fetch that
            // is going to happen anyway.
            if (cacheUpdatedAt == null) return@outcomeCatching read(groupId, Source.SERVER)

            val serverUpdatedAt = groupSyncFirestore.get(groupId).valueOrNull()
                ?.groupUpdatedAt.toInstantOrNull()

            if (serverUpdatedAt == null || serverUpdatedAt <= cacheUpdatedAt) cached
            else read(groupId, Source.SERVER)
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

    private suspend fun read(groupId: GroupId, source: Source): DocHolder<GroupDocument>? {
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
