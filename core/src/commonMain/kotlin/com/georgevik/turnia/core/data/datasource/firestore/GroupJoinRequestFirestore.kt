package com.georgevik.turnia.core.data.datasource.firestore

import com.georgevik.turnia.core.data.datasource.firestore.analytics.trackData
import com.georgevik.turnia.core.data.datasource.firestore.analytics.trackWrite
import com.georgevik.turnia.core.data.datasource.firestore.doc.DocHolder
import com.georgevik.turnia.core.data.datasource.firestore.doc.JoinRequestDocument
import com.georgevik.turnia.core.data.datasource.firestore.errors.GenericFirestoreError
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.GroupId
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.outcomeCatching
import dev.gitlive.firebase.firestore.FirebaseFirestore

/**
 * Interacts with Firestore: `groups/{groupId}/joinRequests`
 *
 * Only an admin can read them, and only `requestToJoinGroup` creates them; the client deletes one
 * to reject it, while accepting goes through the `acceptJoinRequest` function.
 */
class GroupJoinRequestFirestore(private val firestore: FirebaseFirestore) {

    suspend fun get(
        groupId: GroupId,
    ): Outcome<List<DocHolder<JoinRequestDocument>>, GenericFirestoreError> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) {
            val snapshot = requests(groupId).get().trackData(TAG)
            Logger.d(TAG, "Join requests: ${snapshot.documents.size}")

            snapshot.documents.map {
                DocHolder(id = it.reference.id, doc = it.data(JoinRequestDocument.serializer()))
            }
        }

    suspend fun delete(groupId: GroupId, userId: UserId): Outcome<Unit, GenericFirestoreError> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) {
            Logger.d(TAG, "Delete join request")

            requests(groupId).document(userId.value).delete()
            trackWrite(TAG)
        }

    private fun requests(groupId: GroupId) =
        firestore.collection(PATH_JOIN_REQUESTS(groupId.value))

    companion object {
        private const val TAG = "GroupJoinRequestFirestore"
        private fun PATH_JOIN_REQUESTS(groupId: String) = "groups/$groupId/joinRequests"
    }
}
