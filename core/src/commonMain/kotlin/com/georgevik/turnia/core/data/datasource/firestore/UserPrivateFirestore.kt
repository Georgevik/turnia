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
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore

/**
 * Interacts with Firestore: `users/{uid}/private`
 *
 * Everything the owner alone may see. `subscription` lives in its own document because the client
 * may read it but never write it — only the receipt-verification Cloud Function does.
 */
class UserPrivateFirestore(
    private val firestore: FirebaseFirestore
) {

    suspend fun fetchAccount(uid: UserId): Outcome<UserPrivateDocument?, UserProfileError> =
        outcomeCatching({ UserProfileError.LoadFailed(it) }) {
            val snapshot = document(uid, DOCUMENT_ACCOUNT).get().trackData(TAG)
            Logger.d(TAG, "Fetch private account from cache: ${snapshot.metadata.isFromCache}")

            if (!snapshot.exists) null
            else snapshot.data(UserPrivateDocument.serializer())
        }

    suspend fun fetchSubscription(uid: UserId): Outcome<SubscriptionDocument?, UserProfileError> =
        outcomeCatching({ UserProfileError.LoadFailed(it) }) {
            val snapshot = document(uid, DOCUMENT_SUBSCRIPTION).get().trackData(TAG)
            Logger.d(TAG, "Fetch subscription from cache: ${snapshot.metadata.isFromCache}")

            if (!snapshot.exists) null
            else snapshot.data(SubscriptionDocument.serializer())
        }

    /**
     * Seeds the private document for a brand-new account.
     */
    suspend fun createAccount(uid: UserId, email: String): Outcome<Unit, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            Logger.i(TAG, "Create private account document")
            document(uid, DOCUMENT_ACCOUNT).set(
                mapOf(UserPrivateDocument.FIELD_EMAIL to email),
                merge = true,
            )
            trackWrite(TAG)
        }

    /**
     * Registers this device for push.
     */
    suspend fun addFcmToken(uid: UserId, token: String): Outcome<Unit, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            Logger.i(TAG, "Register the push token")
            document(uid, DOCUMENT_ACCOUNT).set(
                mapOf(UserPrivateDocument.FIELD_FCM_TOKENS to FieldValue.arrayUnion(token)),
                merge = true,
            )
            trackWrite(TAG)
        }

    suspend fun removeFcmToken(uid: UserId, token: String): Outcome<Unit, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            Logger.i(TAG, "Unregister the push token")
            document(uid, DOCUMENT_ACCOUNT).updateFields {
                UserPrivateDocument.FIELD_FCM_TOKENS to FieldValue.arrayRemove(token)
            }
            trackWrite(TAG)
        }

    suspend fun fetchJoinRequests(uid: UserId): Outcome<List<String>, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            val snapshot = document(uid, DOCUMENT_JOIN_REQUESTS).get().trackData(TAG)
            Logger.d(TAG, "Fetch join requests from cache: ${snapshot.metadata.isFromCache}")

            if (!snapshot.exists) emptyList()
            else snapshot.data(UserJoinRequestsDocument.serializer()).groupIds
        }

    suspend fun removeJoinRequest(uid: UserId, groupId: String): Outcome<Unit, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            Logger.i(TAG, "Drop the join request pointer")
            document(uid, DOCUMENT_JOIN_REQUESTS).set(
                mapOf(UserJoinRequestsDocument.FIELD_GROUP_IDS to FieldValue.arrayRemove(groupId)),
                merge = true,
            )
            trackWrite(TAG)
        }

    suspend fun setNotificationsEnabled(
        uid: UserId,
        enabled: Boolean
    ): Outcome<Unit, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            Logger.i(TAG, "Set notifications enabled: $enabled")
            document(uid, DOCUMENT_ACCOUNT).set(
                mapOf(UserPrivateDocument.FIELD_NOTIFICATIONS_ENABLED to enabled),
                merge = true,
            )
            trackWrite(TAG)
        }

    private fun document(uid: UserId, documentId: String) =
        firestore.collection(PATH_PRIVATE(uid.value)).document(documentId)

    companion object {
        private const val TAG = "UserPrivateFirestore"
        private const val DOCUMENT_ACCOUNT = "account"
        private const val DOCUMENT_SUBSCRIPTION = "subscription"
        private const val DOCUMENT_JOIN_REQUESTS = "joinRequests"
        private fun PATH_PRIVATE(uid: String) = "users/${uid}/private"
    }
}
