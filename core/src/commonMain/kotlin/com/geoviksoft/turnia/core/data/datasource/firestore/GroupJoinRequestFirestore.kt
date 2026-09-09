package com.geoviksoft.turnia.core.data.datasource.firestore

import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackData
import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackWrite
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.DocHolder
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.JoinRequestDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.errors.GenericFirestoreError
import com.geoviksoft.turnia.core.data.datasource.firestore.sync.DebouncedReads
import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.outcomeCatching
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlin.time.Duration.Companion.seconds

/**
 * Interacts with Firestore: `groups/{groupId}/joinRequests`
 *
 * Only `requestToJoinGroup` creates them and only `acceptJoinRequest` / `rejectJoinRequest` answer
 * them. An answered request stays behind as a receipt for the requester, who is also the only one
 * who may delete it — to cancel a pending request, or to acknowledge an answered one.
 */
class GroupJoinRequestFirestore(private val firestore: FirebaseFirestore) {

    private val cachedResponse = DebouncedReads<String, JoinRequestDocument>(window = 10.seconds)

    /**
     * What an admin has to answer. Filtered server-side: an answered request lingers until its
     * requester acknowledges it, and it is not waiting on anybody here.
     */
    suspend fun getPending(
        groupId: GroupId,
    ): Outcome<List<DocHolder<JoinRequestDocument>>, GenericFirestoreError> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) {
            val snapshot = requests(groupId)
                .where { JoinRequestDocument.FIELD_STATUS equalTo JoinRequestDocument.STATUS_PENDING }
                .get()
                .trackData(TAG, "pendingRequests")
            Logger.d(TAG, "Pending join requests: ${snapshot.documents.size}")

            snapshot.documents.map {
                DocHolder(id = it.reference.id, doc = it.data(JoinRequestDocument.serializer()))
            }
        }

    /** The caller's own request in a group, which they may read by id and by id alone. */
    suspend fun observe(
        groupId: GroupId,
        userId: UserId,
    ): Outcome<JoinRequestDocument?, GenericFirestoreError> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) {
            val cachedId = "$groupId $userId"
            cachedResponse.cached(cachedId)?.let {  return@outcomeCatching it }

            val snapshot = requests(groupId).document(userId.value).get().trackData(TAG, "myRequest")
            Logger.d(TAG, "Fetch join request from cache: ${snapshot.metadata.isFromCache}")

            val joinRequest = if (!snapshot.exists) null
            else snapshot.data(JoinRequestDocument.serializer())

            joinRequest?.also {
                cachedResponse.remember(cachedId, it)
            }
        }

    suspend fun delete(groupId: GroupId, userId: UserId): Outcome<Unit, GenericFirestoreError> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) {
            Logger.d(TAG, "Delete join request")

            requests(groupId).document(userId.value).delete()
            trackWrite(TAG, "deleteRequest")
        }

    private fun requests(groupId: GroupId) =
        firestore.collection(PATH_JOIN_REQUESTS(groupId.value))

    companion object {
        private const val TAG = "GroupJoinRequestFirestore"
        private fun PATH_JOIN_REQUESTS(groupId: String) = "groups/$groupId/joinRequests"
    }
}
