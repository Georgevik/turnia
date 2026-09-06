package com.georgevik.turnia.core.data.datasource.firestore

import com.georgevik.turnia.core.data.datasource.firestore.analytics.trackData
import com.georgevik.turnia.core.data.datasource.firestore.doc.DocHolder
import com.georgevik.turnia.core.data.datasource.firestore.doc.RevokedGroupDocument
import com.georgevik.turnia.core.data.datasource.firestore.sync.SharedListeners
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.system.toInstantOrNull
import com.georgevik.turnia.core.system.toTimestamp
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull

/**
 * Interacts with Firestore: `users/{uid}/revokedGroups`
 *
 * The groups the user was removed from, each with the event types their leftover events use. Only
 * the Cloud Functions write here — this side reads, and it is the substitute for the group document
 * a revoked user can no longer touch.
 *
 * Being revoked is a rare, deliberate act, so a live listener would spend a read per document to
 * learn nothing almost every time. The cache answers first and `revokedGroups` on the user's sync
 * document says whether anything has moved since; only then is the server asked, and only for what
 * changed.
 */
class RevokedGroupFirestore(
    private val firestore: FirebaseFirestore,
    private val userSyncFirestore: UserSyncFirestore,
    scope: CoroutineScope,
) {
    private val listeners =
        SharedListeners<UserId, List<DocHolder<RevokedGroupDocument>>>(scope)

    fun observe(userId: UserId): Flow<List<DocHolder<RevokedGroupDocument>>> =
        listeners.shared(userId) { revokedGroups(userId) }

    private fun revokedGroups(
        userId: UserId,
    ): Flow<List<DocHolder<RevokedGroupDocument>>> = flow {
        var known = query(userId, null, Source.CACHE)
        emit(known.live())

        emitAll(userSyncFirestore.observe(userId).mapNotNull { sync ->
            val cacheUpdatedAt = known.mapNotNull { it.doc.updateAt.toInstantOrNull() }.maxOrNull()
            val serverUpdatedAt =
                sync.revokedGroupsUpdatedAt.toInstantOrNull() ?: return@mapNotNull null

            val settled = cacheUpdatedAt != null && cacheUpdatedAt >= serverUpdatedAt
            if (settled) return@mapNotNull known.live()

            val changed = query(userId, cacheUpdatedAt?.toTimestamp(), Source.SERVER)
                .associateBy { it.id }
                .toMutableMap()

            val merged = known.map { cached -> changed.remove(cached.id) ?: cached }
            known = merged + changed.values
            known.live()
        })
    }.catch { throwable ->
        Logger.e(TAG, "Revoked groups failed", throwable)
        emit(emptyList())
    }

    private suspend fun query(
        userId: UserId,
        sinceUpdateAt: Timestamp?,
        source: Source,
    ): List<DocHolder<RevokedGroupDocument>> {
        val since = sinceUpdateAt ?: Timestamp(0, 0)
        val snapshot = firestore.collection(PATH_REVOKED_GROUPS(userId.value)).where {
            RevokedGroupDocument.FIELD_UPDATE_AT greaterThan since
        }.get(source).trackData(TAG, "revokedGroups($source)")

        Logger.i(TAG, "Revoked groups. Source: $source. Amount: ${snapshot.documents.size}")

        return snapshot.documents.map {
            DocHolder(id = it.reference.id, doc = it.data(RevokedGroupDocument.serializer()))
        }
    }

    private fun List<DocHolder<RevokedGroupDocument>>.live() = filterNot { it.doc.isDeleted }

    companion object {
        private const val TAG = "RevokedGroupFirestore"
        private fun PATH_REVOKED_GROUPS(uid: String) = "users/$uid/revokedGroups"
    }
}
