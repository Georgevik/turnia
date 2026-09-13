package com.geoviksoft.turnia.core.data.datasource.firestore

import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackData
import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackWrite
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.DocHolder
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.JoinRequestDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.JoinRequestStatusDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.errors.GenericFirestoreError
import com.geoviksoft.turnia.core.data.datasource.firestore.sync.DebouncedReads
import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.outcomeCatching
import com.geoviksoft.turnia.core.system.toInstantOrNull
import com.geoviksoft.turnia.core.system.valueOrNull
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Interacts with Firestore: `groups/{groupId}/joinRequests`
 *
 * Only `requestToJoinGroup` creates them and only `acceptJoinRequest` / `rejectJoinRequest` answer
 * them. An answered request stays behind as a receipt for the requester, who is also the only one
 * who may delete it — to cancel a pending request, or to acknowledge an answered one.
 */
class GroupJoinRequestFirestore(
    private val firestore: FirebaseFirestore,
    private val groupSyncFirestore: GroupSyncFirestore,
) {

    private val cachedResponse = DebouncedReads<String, JoinRequestDocument>(window = 10.seconds)

    /**
     * What an admin has to answer, kept current: the requests the group's pending map names, each
     * from the cache unless its entry is newer than the copy held. A group nobody has asked to join
     * costs no read, and an answer or a new request arrives through the sync listener.
     */
    fun observePending(groupId: GroupId): Flow<List<DocHolder<JoinRequestDocument>>> =
        groupSyncFirestore.observe(groupId)
            .map { it.pendingJoinRequests }
            .distinctUntilChanged()
            .map { pending ->
                Logger.d(TAG, "Pending join requests: ${pending.size}")
                pending.mapNotNull { (uid, requestedAt) ->
                    outcomeCatching(TAG, { it }) {
                        pendingRequest(groupId, uid, requestedAt.toInstantOrNull())
                    }.valueOrNull()
                }
            }

    private suspend fun pendingRequest(
        groupId: GroupId,
        uid: String,
        requestedAt: Instant?,
    ): DocHolder<JoinRequestDocument>? {
        val document = requests(groupId).document(uid)
        val cached = document.getCached(TAG, "pendingRequest(cache)")
            ?.data(JoinRequestDocument.serializer())
        val cachedAt = cached?.requestedAt.toInstantOrNull()

        val request = if (cached != null && (requestedAt == null || (cachedAt != null && cachedAt >= requestedAt))) {
            cached
        } else {
            val snapshot = document.get(Source.SERVER).trackData(TAG, "pendingRequest(server)")
            if (snapshot.exists) snapshot.data(JoinRequestDocument.serializer()) else null
        }

        return request
            ?.takeIf { it.status == JoinRequestStatusDocument.PENDING }
            ?.let { DocHolder(id = uid, doc = it) }
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
