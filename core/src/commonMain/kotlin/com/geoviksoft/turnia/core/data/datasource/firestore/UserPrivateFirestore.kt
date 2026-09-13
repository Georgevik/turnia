package com.geoviksoft.turnia.core.data.datasource.firestore

import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackData
import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackWrite
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.SubscriptionDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.UserJoinRequestsDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.UserPreferencesDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.UserPrivateDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.errors.UserProfileError
import com.geoviksoft.turnia.core.data.datasource.firestore.sync.SharedListeners
import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.core.domain.model.EventTypeId
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.outcomeCatching
import com.geoviksoft.turnia.core.system.toInstantOrNull
import com.geoviksoft.turnia.core.system.valueOrNull
import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.DocumentReference
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.minutes

/**
 * Interacts with Firestore: `users/{uid}/private`
 */
class UserPrivateFirestore(
    private val firestore: FirebaseFirestore,
    private val userSyncFirestore: UserSyncFirestore,
    scope: CoroutineScope,
) {
    private val preferences =
        SharedListeners<UserId, UserPreferencesDocument>(scope, keepAlive = 10.minutes)

    /**
     * The user's own picks, kept current off the `preferences` marker rather than a listener of
     * their own.
     */
    fun observePreferences(uid: UserId): Flow<UserPreferencesDocument> =
        preferences.shared(uid) { preferenceUpdates(uid) }

    /** The current picks, off the same marker-gated flow: every group screen asks. */
    suspend fun fetchCachedPreferences(uid: UserId): Outcome<UserPreferencesDocument, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            observePreferences(uid).first()
        }

    suspend fun updateTypeColor(
        uid: UserId,
        groupId: GroupId,
        typeId: EventTypeId,
        color: String,
    ): Outcome<Unit, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            Logger.i(TAG, "Update group event type colour")
            val key = UserPreferencesDocument.typeColorKey(groupId.value, typeId.value)

            val batch = firestore.batch()
            // `set(merge)` and not `updateFields`: the document does not exist until the first pick.
            // Firestore merges map fields key by key, so the other types keep their colours.
            batch.set(
                document(DOCUMENT_PREFERENCES, uid),
                UserPreferencesDocument(groupEventTypeColors = mapOf(key to color)),
                merge = true,
            )
            val syncWrite = userSyncFirestore.writePreferences(batch, uid)
            batch.commit()
            trackWrite(TAG, "updateTypeColor")
            syncWrite.committed()
        }

    private fun preferenceUpdates(uid: UserId): Flow<UserPreferencesDocument> =
        userSyncFirestore.observe(uid)
            .map { sync -> currentPreferences(uid, sync.preferencesUpdatedAt) }
            .distinctUntilChanged()
            .catch { throwable ->
                Logger.e(TAG, "Preferences updates failed", throwable)
            }

    /**
     * The cache whenever the marker says it is current, the server only when it is not.
     */
    private suspend fun currentPreferences(
        uid: UserId,
        marker: BaseTimestamp?,
    ): UserPreferencesDocument {
        val cached = cachedPreferences(uid)
        if (cached != null && cached.isSettledAgainst(marker)) return cached

        // Nothing has ever been written: no marker to be behind, and no document to read. The cache
        // cannot say so itself — a missing document reads as a miss — so without this check a user
        // who never picked a colour paid a server read on every call.
        if (marker.toInstantOrNull() == null) return UserPreferencesDocument(updateAt = null)

        return serverPreferences(uid)
    }

    private fun UserPreferencesDocument.isSettledAgainst(marker: BaseTimestamp?): Boolean {
        val markerAt = marker.toInstantOrNull() ?: return true
        val mineAt = updateAt.toInstantOrNull() ?: return false
        return mineAt >= markerAt
    }

    private suspend fun cachedPreferences(uid: UserId): UserPreferencesDocument? =
        document(DOCUMENT_PREFERENCES, uid).getCached(TAG, "preferences(cache)")
            ?.data(UserPreferencesDocument.serializer())

    private suspend fun serverPreferences(uid: UserId): UserPreferencesDocument {
        val snapshot = document(DOCUMENT_PREFERENCES, uid).get(Source.SERVER)
            .trackData(TAG, "preferences(server)")

        // The marker says a document was written, yet it may since be gone: an empty one, then.
        return if (!snapshot.exists) UserPreferencesDocument(updateAt = null)
        else snapshot.data(UserPreferencesDocument.serializer())
    }

    suspend fun fetchAccount(uid: UserId): Outcome<UserPrivateDocument?, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            val cached = cachedAccount(uid)
            if (cached != null && isSettled(uid, cached)) {
                Logger.d(TAG, "Private account is settled, no read")
                return@outcomeCatching cached
            }

            val snapshot = document(DOCUMENT_ACCOUNT, uid).get(Source.SERVER).trackData(TAG, "account(server)")
            Logger.i(TAG, "Private account read from the server")

            if (!snapshot.exists) null
            else snapshot.data(UserPrivateDocument.serializer())
        }

    /**
     * The entitlement from the cache while the server-only `subscription` marker says it is current.
     * No marker means the server never wrote one: an absent document, the free tier, and no read.
     */
    suspend fun fetchSubscription(uid: UserId): Outcome<SubscriptionDocument?, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            val cached = document(DOCUMENT_SUBSCRIPTION, uid).getCached(TAG, "subscription(cache)")
                ?.data(SubscriptionDocument.serializer())
            val marker = userSyncFirestore.get(uid).valueOrNull()
                ?.subscriptionUpdatedAt.toInstantOrNull()

            val cachedAt = cached?.updatedAt
            if (marker == null || (cachedAt != null && cachedAt >= marker)) {
                Logger.d(TAG, "Subscription is settled, no read")
                return@outcomeCatching cached
            }

            val snapshot = document(DOCUMENT_SUBSCRIPTION, uid).get(Source.SERVER)
                .trackData(TAG, "subscription(server)")
            Logger.i(TAG, "Subscription read from the server")

            if (!snapshot.exists) null
            else snapshot.data(SubscriptionDocument.serializer())
        }

    /**
     * Seeds the private document for a brand-new account.
     */
    suspend fun createAccount(uid: UserId, email: String): Outcome<Unit, UserProfileError> =
        patchAccount(uid, "Create private account document", AccountPatch(email = email))

    /** Registers this device for push. */
    suspend fun addFcmToken(uid: UserId, token: String): Outcome<Unit, UserProfileError> =
        patchAccount(
            uid,
            "Register the push token",
            AccountPatch(fcmTokens = FieldValue.arrayUnion(token)),
        )

    suspend fun removeFcmToken(uid: UserId, token: String): Outcome<Unit, UserProfileError> =
        patchAccount(
            uid,
            "Unregister the push token",
            AccountPatch(fcmTokens = FieldValue.arrayRemove(token)),
        )

    fun fetchJoinRequests(uid: UserId): Flow<List<String>> = flow {
        var cacheDoc: UserJoinRequestsDocument? =
            document(DOCUMENT_JOIN_REQUESTS, uid).getCached(TAG, "joinRequests(cache)")
                ?.data(UserJoinRequestsDocument.serializer())

        emit(cacheDoc?.groupIds.orEmpty())

        emitAll(
            userSyncFirestore.observe(uid).mapNotNull { sync ->
                val cacheUpdateAt = cacheDoc?.updateAt?.toInstantOrNull()
                val syncUpdateAt =
                    sync.joinRequestsMarker.toInstantOrNull() ?: return@mapNotNull null

                if (cacheUpdateAt != null && cacheUpdateAt >= syncUpdateAt) {
                    return@mapNotNull null
                }

                val serverSnapshot =
                    document(DOCUMENT_JOIN_REQUESTS, uid).get(Source.SERVER).trackData(TAG, "joinRequests(server)")
                val serverDoc: UserJoinRequestsDocument? = if (serverSnapshot.exists) {
                    serverSnapshot.data(UserJoinRequestsDocument.serializer())
                } else null

                cacheDoc = serverDoc
                cacheDoc?.groupIds.orEmpty()
            })
    }

    suspend fun removeJoinRequest(uid: UserId, groupId: String): Outcome<Unit, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            Logger.i(TAG, "Drop the join request pointer")

            val batch = firestore.batch()
            batch.set(
                document(DOCUMENT_JOIN_REQUESTS, uid),
                mapOf(
                    UserJoinRequestsDocument.FIELD_GROUP_IDS to FieldValue.arrayRemove(groupId),
                    UserJoinRequestsDocument.FIELD_UPDATE_AT to FieldValue.serverTimestamp,
                ),
                merge = true,
            )
            val syncWrite = userSyncFirestore.writeJoinRequests(batch, uid)
            batch.commit()
            trackWrite(TAG, "removeJoinRequest")
            syncWrite.committed()
        }

    suspend fun setNotificationsEnabled(
        uid: UserId, enabled: Boolean
    ): Outcome<Unit, UserProfileError> = patchAccount(
        uid,
        "Set notifications enabled: $enabled",
        AccountPatch(notificationsEnabled = enabled),
    )

    /**
     * Merges [patch] into `private/account` and bumps the sync marker in the same commit.
     */
    private suspend fun patchAccount(
        uid: UserId,
        log: String,
        patch: AccountPatch,
    ): Outcome<Unit, UserProfileError> = outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
        Logger.i(TAG, log)

        val batch = firestore.batch()
        // Without defaults, so the fields the patch does not carry are not encoded at all.
        batch.set(
            document(DOCUMENT_ACCOUNT, uid),
            patch.copy(updateAt = Timestamp.ServerTimestamp),
            merge = true,
        ) { encodeDefaults = false }
        val syncWrite = userSyncFirestore.writeAccount(batch, uid)
        batch.commit()
        trackWrite(TAG, "patchAccount")
        syncWrite.committed()
    }

    private suspend fun isSettled(uid: UserId, cached: UserPrivateDocument): Boolean {
        val serverUpdatedAt =
            userSyncFirestore.get(uid).valueOrNull()?.accountMarker.toInstantOrNull()
                ?: return true

        val cacheUpdatedAt = cached.updateAt.toInstantOrNull() ?: return false
        return cacheUpdatedAt >= serverUpdatedAt
    }

    private suspend fun cachedAccount(uid: UserId): UserPrivateDocument? =
        document(DOCUMENT_ACCOUNT, uid).getCached(TAG, "account(cache)")
            ?.data(UserPrivateDocument.serializer())

    private fun document(documentId: String, uid: UserId): DocumentReference =
        firestore.collection(PATH_PRIVATE(uid.value)).document(documentId)

    @Serializable
    private data class AccountPatch(
        @SerialName(UserPrivateDocument.FIELD_EMAIL) val email: String? = null,
        @SerialName(UserPrivateDocument.FIELD_FCM_TOKENS) val fcmTokens: FieldValue? = null,
        @SerialName(UserPrivateDocument.FIELD_NOTIFICATIONS_ENABLED) val notificationsEnabled: Boolean? = null,
        @SerialName(UserPrivateDocument.FIELD_UPDATE_AT) val updateAt: BaseTimestamp? = null,
    )

    companion object {
        private const val TAG = "UserPrivateFirestore"
        private const val DOCUMENT_ACCOUNT = "account"
        private const val DOCUMENT_SUBSCRIPTION = "subscription"
        private const val DOCUMENT_JOIN_REQUESTS = "joinRequests"
        private const val DOCUMENT_PREFERENCES = "preferences"
        private fun PATH_PRIVATE(uid: String) = "users/${uid}/private"
    }
}
