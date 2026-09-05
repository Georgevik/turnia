package com.georgevik.turnia.core.data.datasource.firestore

import com.georgevik.turnia.core.data.datasource.firestore.analytics.trackData
import com.georgevik.turnia.core.data.datasource.firestore.doc.DocHolder
import com.georgevik.turnia.core.data.datasource.firestore.doc.RevokedGroupDocument
import com.georgevik.turnia.core.data.datasource.firestore.sync.SharedListeners
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.UserId
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Interacts with Firestore: `users/{uid}/revokedGroups`
 *
 * The groups the user was removed from, each with the event types their leftover events use. Only
 * the Cloud Functions write here — this side reads, and it is the substitute for the group document
 * a revoked user can no longer touch.
 */
class RevokedGroupFirestore(
    private val firestore: FirebaseFirestore,
    scope: CoroutineScope,
) {
    private val revokedGroupsFlow =
        SharedListeners<UserId, List<DocHolder<RevokedGroupDocument>>>(scope)

    fun observe(userId: UserId): Flow<List<DocHolder<RevokedGroupDocument>>> =
        revokedGroupsFlow.shared(userId) { snapshotRevokedGroups(userId) }

    private fun snapshotRevokedGroups(
        userId: UserId,
    ): Flow<List<DocHolder<RevokedGroupDocument>>> = revokedGroups(userId).snapshots
        .map { snapshot ->
            snapshot.trackData(TAG)
            Logger.d(TAG, "Revoked groups of the user: ${snapshot.documents.size}")

            snapshot.documents.map {
                DocHolder(id = it.reference.id, doc = it.data(RevokedGroupDocument.serializer()))
            }
        }
        .catch { throwable ->
            Logger.e(TAG, "Revoked groups listener failed", throwable)
            emit(emptyList())
        }

    private fun revokedGroups(userId: UserId) =
        firestore.collection(PATH_REVOKED_GROUPS(userId.value))

    companion object {
        private const val TAG = "RevokedGroupFirestore"
        private fun PATH_REVOKED_GROUPS(uid: String) = "users/$uid/revokedGroups"
    }
}
