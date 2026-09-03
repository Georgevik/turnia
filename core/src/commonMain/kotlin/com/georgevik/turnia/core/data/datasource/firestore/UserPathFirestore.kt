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
            mapper.map(snapshot)
        }

    suspend fun fetchCalendarsSharedWithMe(uid: String): Outcome<List<UserProfile>, UserProfileError> =
        outcomeCatching({ UserProfileError.LoadFailed(it) }) {
            val snapshot = firestore.collection(PATH_USER).where {
                UserDocument.FIELD_CALENDAR_SHARED_WITH contains uid
            }.get()
            Logger.d(TAG, "Calendars shared with me: ${snapshot.documents.size}")

            snapshot.documents.map { mapper.map(it) }
        }

    suspend fun getUserDocument(uid: String): Outcome<UserDocument, UserProfileError> =
        outcomeCatching({ UserProfileError.LoadFailed(it) }) {
            val snapshot = firestore.collection(PATH_USER).document(uid).get()
            if (!snapshot.exists) return Outcome.Failure(UserProfileError.NotFound)

            snapshot.data(UserDocument.serializer())
        }

    suspend fun updateProfile(
        uid: String,
        name: String,
        username: String
    ): Outcome<Unit, UserProfileError> =
        outcomeCatching({ UserProfileError.LoadFailed(it) }) {
            Logger.i(TAG, "Update public profile")
            firestore.collection(PATH_USER).document(uid).updateFields {
                UserDocument.FIELD_NAME to name
                UserDocument.FIELD_USERNAME to username
            }
        }

    suspend fun updateUsername(uid: String, username: String): Outcome<Unit, UserProfileError> =
        outcomeCatching({ UserProfileError.LoadFailed(it) }) {
            Logger.i(TAG, "Update username")
            firestore.collection(PATH_USER).document(uid).updateFields {
                UserDocument.FIELD_USERNAME to username
            }
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
