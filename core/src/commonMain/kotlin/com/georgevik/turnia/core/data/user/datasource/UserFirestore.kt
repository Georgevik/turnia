package com.georgevik.turnia.core.data.user.datasource

import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.data.user.UserFactory
import com.georgevik.turnia.core.domain.model.UserProfile
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.outcomeCatching
import dev.gitlive.firebase.firestore.FirebaseFirestore

sealed class UserProfileError {
    data object NotFound : UserProfileError()
    data class LoadFailed(val error: Throwable) : UserProfileError()
}

/**
 * Interacts with Firestore: `users/{uid}`
 */
class UserProfileFirestore(
    private val firestore: FirebaseFirestore,
    private val userFactory: UserFactory
) {

    suspend fun fetch(uid: String): Outcome<UserProfile, UserProfileError> =
        outcomeCatching({ UserProfileError.LoadFailed(it) }) {
            val snapshot = firestore.collection(PATH_USER).document(uid).get()
            Logger.d(TAG, "Fetch user from cache: ${snapshot.metadata.isFromCache}")

            if (!snapshot.exists) return Outcome.Failure(UserProfileError.NotFound)
            val userDocument = snapshot.data(UserDocument.serializer())
            userFactory.create(uid, userDocument)
        }

    suspend fun update(uid: String, userPatched: UserDocument): Outcome<Unit, UserProfileError> =
        outcomeCatching({ UserProfileError.LoadFailed(it) }) {
            firestore.collection(PATH_USER).document(uid).set(userPatched)
        }

    companion object {
        private const val TAG = "UserProfileFirestore"
        private const val PATH_USER = "users"
    }
}
