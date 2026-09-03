package com.georgevik.turnia.core.data.datasource.firestore

import com.georgevik.turnia.core.data.datasource.firestore.doc.UserDocument
import com.georgevik.turnia.core.data.datasource.firestore.errors.UserProfileError
import com.georgevik.turnia.core.data.datasource.firestore.mappers.UserDocumentMapper
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.UserProfile
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.outcomeCatching
import dev.gitlive.firebase.firestore.FirebaseFirestore

/**
 * Interacts with Firestore: `users/{uid}`
 */
class UserPathFirestore(
    private val firestore: FirebaseFirestore,
    private val mapper: UserDocumentMapper,
) {

    suspend fun fetch(uid: String): Outcome<UserProfile, UserProfileError> =
        outcomeCatching({ UserProfileError.LoadFailed(it) }) {
            val snapshot = firestore.collection(PATH_USER).document(uid).get()
            Logger.d(TAG, "Fetch user from cache: ${snapshot.metadata.isFromCache}")

            if (!snapshot.exists) return Outcome.Failure(UserProfileError.NotFound)
            val userDocument = snapshot.data(UserDocument.serializer())
            mapper.map(uid, userDocument)
        }

    suspend fun update(uid: String, userPatched: UserDocument): Outcome<Unit, UserProfileError> =
        outcomeCatching({ UserProfileError.LoadFailed(it) }) {
            Logger.i(TAG, "Update user document")
            firestore.collection(PATH_USER).document(uid).set(userPatched)
        }

    companion object {
        private const val TAG = "UserPathFirestore"
        private const val PATH_USER = "users"
    }
}
