package com.georgevik.turnia.core.data.datasource.firestore

import com.georgevik.turnia.core.data.datasource.firestore.analytics.trackData
import com.georgevik.turnia.core.data.datasource.firestore.analytics.trackWrite
import com.georgevik.turnia.core.data.datasource.firestore.doc.SubscriptionDocument
import com.georgevik.turnia.core.data.datasource.firestore.doc.UserJoinRequestsDocument
import com.georgevik.turnia.core.data.datasource.firestore.doc.UserPrivateDocument
import com.georgevik.turnia.core.data.datasource.firestore.errors.UserProfileError
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.outcomeCatching
import com.georgevik.turnia.core.system.toInstantOrNull
import com.georgevik.turnia.core.system.valueOrNull
import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.DocumentReference
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Interacts with Firestore: `users/{uid}/private`
 */
class UserPrivateFirestore(
    private val firestore: FirebaseFirestore,
    private val userSyncFirestore: UserSyncFirestore,
) {

    suspend fun fetchAccount(uid: UserId): Outcome<UserPrivateDocument?, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            val cached = cachedAccount(uid)
            if (cached != null && isSettled(uid, cached)) {
                Logger.d(TAG, "Private account is settled, no read")
                return@outcomeCatching cached
            }

            val snapshot = document(DOCUMENT_ACCOUNT, uid).get(Source.SERVER).trackData(TAG)
            Logger.i(TAG, "Private account read from the server")

            if (!snapshot.exists) null
            else snapshot.data(UserPrivateDocument.serializer())
        }

    suspend fun fetchSubscription(uid: UserId): Outcome<SubscriptionDocument?, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            val snapshot = document(DOCUMENT_SUBSCRIPTION, uid).get().trackData(TAG)
            Logger.d(TAG, "Fetch subscription from cache: ${snapshot.metadata.isFromCache}")

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
        var cacheDoc: UserJoinRequestsDocument? = null
        try {
            val snapshot = document(DOCUMENT_JOIN_REQUESTS, uid).get(Source.CACHE).trackData(TAG)
            cacheDoc = if (snapshot.exists) {
                snapshot.data(UserJoinRequestsDocument.serializer())
            } else null
        } catch (exception: Exception) {
            Logger.e(TAG, "Failed to read the join requests", exception)
        }


        emit(cacheDoc?.groupIds.orEmpty())

        emitAll(
            userSyncFirestore.observe(uid).mapNotNull { sync ->
                val cacheUpdateAt = cacheDoc?.updateAt?.toInstantOrNull()
                val syncUpdateAt =
                    sync.privateUpdatedAt?.toInstantOrNull() ?: return@mapNotNull null

                if (cacheUpdateAt != null && cacheUpdateAt >= syncUpdateAt) {
                    return@mapNotNull null
                }

                val serverSnapshot =
                    document(DOCUMENT_JOIN_REQUESTS, uid).get(Source.SERVER).trackData(TAG)
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
            val syncWrite = userSyncFirestore.writePrivate(batch, uid)
            batch.commit()
            trackWrite(TAG)
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
        val syncWrite = userSyncFirestore.writePrivate(batch, uid)
        batch.commit()
        trackWrite(TAG)
        syncWrite.committed()
    }

    private suspend fun isSettled(uid: UserId, cached: UserPrivateDocument): Boolean {
        val serverUpdatedAt =
            userSyncFirestore.get(uid).valueOrNull()?.privateUpdatedAt.toInstantOrNull()
                ?: return true

        val cacheUpdatedAt = cached.updateAt.toInstantOrNull() ?: return false
        return cacheUpdatedAt >= serverUpdatedAt
    }

    /** A document the cache does not have makes the read fail rather than come back empty. */
    private suspend fun cachedAccount(uid: UserId): UserPrivateDocument? = try {
        document(DOCUMENT_ACCOUNT, uid).get(Source.CACHE).trackData(TAG).takeIf { it.exists }
            ?.data(UserPrivateDocument.serializer())
    } catch (exception: Exception) {
        Logger.d(TAG, "The private account is not cached yet: ${exception.message}")
        null
    }

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
        private fun PATH_PRIVATE(uid: String) = "users/${uid}/private"
    }
}
