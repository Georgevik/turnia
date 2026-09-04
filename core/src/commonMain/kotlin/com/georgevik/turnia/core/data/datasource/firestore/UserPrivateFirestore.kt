package com.georgevik.turnia.core.data.datasource.firestore

import com.georgevik.turnia.core.data.datasource.firestore.doc.SubscriptionDocument
import com.georgevik.turnia.core.data.datasource.firestore.doc.UserPrivateDocument
import com.georgevik.turnia.core.data.datasource.firestore.errors.UserProfileError
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.outcomeCatching
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
            val snapshot = document(uid, DOCUMENT_ACCOUNT).get()
            Logger.d(TAG, "Fetch private account from cache: ${snapshot.metadata.isFromCache}")

            if (!snapshot.exists) null
            else snapshot.data(UserPrivateDocument.serializer())
        }

    suspend fun fetchSubscription(uid: UserId): Outcome<SubscriptionDocument?, UserProfileError> =
        outcomeCatching({ UserProfileError.LoadFailed(it) }) {
            val snapshot = document(uid, DOCUMENT_SUBSCRIPTION).get()
            Logger.d(TAG, "Fetch subscription from cache: ${snapshot.metadata.isFromCache}")

            if (!snapshot.exists) null
            else snapshot.data(SubscriptionDocument.serializer())
        }

    suspend fun updateAccount(
        uid: UserId,
        account: UserPrivateDocument
    ): Outcome<Unit, UserProfileError> =
        outcomeCatching({ UserProfileError.LoadFailed(it) }) {
            Logger.i(TAG, "Update private account document")
            document(uid, DOCUMENT_ACCOUNT).set(account)
        }

    private fun document(uid: UserId, documentId: String) =
        firestore.collection(PATH_PRIVATE(uid.value)).document(documentId)

    companion object {
        private const val TAG = "UserPrivateFirestore"
        private const val DOCUMENT_ACCOUNT = "account"
        private const val DOCUMENT_SUBSCRIPTION = "subscription"
        private fun PATH_PRIVATE(uid: String) = "users/${uid}/private"
    }
}
