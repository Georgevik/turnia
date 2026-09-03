package com.georgevik.turnia.core.data.datasource.firestore

import com.georgevik.turnia.core.data.datasource.firestore.doc.UsernameDocument
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.UserProfile
import com.georgevik.turnia.core.domain.model.UsernameError
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.outcomeCatching
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.FirebaseFirestoreException
import dev.gitlive.firebase.firestore.FirestoreExceptionCode
import dev.gitlive.firebase.firestore.code

/**
 * Interacts with Firestore: `usernames/{username}`
 */
class UsernameFirestore(
    private val firestore: FirebaseFirestore
) {

    suspend fun claim(
        username: String,
        uid: String,
        name: String
    ): Outcome<Unit, UsernameError> =
        outcomeCatching({ throwable -> throwable.toClaimError(username) }) {
            Logger.i(TAG, "Claim username")
            firestore.collection(PATH_USERNAMES).document(username)
                .set(UsernameDocument(username = username, uid = uid, name = name))
        }

    suspend fun release(username: String): Outcome<Unit, UsernameError> =
        outcomeCatching({ UsernameError.SaveFailed }) {
            Logger.i(TAG, "Release username")
            firestore.collection(PATH_USERNAMES).document(username).delete()
        }

    suspend fun findByUids(uids: List<String>): Outcome<List<UserProfile>, UsernameError> =
        outcomeCatching({ UsernameError.SaveFailed }) {
            uids.chunked(UID_QUERY_CHUNK).flatMap { chunk ->
                val snapshot = firestore.collection(PATH_USERNAMES)
                    .where { UsernameDocument.FIELD_UID inArray chunk }
                    .get()
                Logger.d(TAG, "Resolved ${snapshot.documents.size} of ${chunk.size} uids")

                snapshot.documents.map { it.data(UsernameDocument.serializer()).toProfile() }
            }
        }

    suspend fun search(
        prefix: String,
        limit: Int = SEARCH_LIMIT
    ): Outcome<List<UserProfile>, UsernameError> =
        outcomeCatching({ UsernameError.SaveFailed }) {
            if (prefix.length < MIN_SEARCH_LENGTH) return@outcomeCatching emptyList()

            val snapshot = firestore.collection(PATH_USERNAMES)
                .where {
                    (UsernameDocument.FIELD_USERNAME greaterThanOrEqualTo prefix) and
                            (UsernameDocument.FIELD_USERNAME lessThanOrEqualTo prefix + '￿')
                }
                .limit(limit)
                .get()
            Logger.d(TAG, "Username search '$prefix': ${snapshot.documents.size} results")

            snapshot.documents.map { it.data(UsernameDocument.serializer()).toProfile() }
        }

    /**
     * UsernameDocument contains the minimum info for unknown external users
     */
    private fun UsernameDocument.toProfile() =
        UserProfile(id = uid, name = name, username = username)

    private fun Throwable.toClaimError(username: String): UsernameError =
        if (this is FirebaseFirestoreException && code == FirestoreExceptionCode.PERMISSION_DENIED) {
            Logger.i(TAG, "Username '$username' already taken")
            UsernameError.Taken
        } else {
            Logger.e(TAG, "Could not claim username '$username'", this)
            UsernameError.SaveFailed
        }

    companion object {
        private const val TAG = "UsernameFirestore"
        private const val PATH_USERNAMES = "usernames"
        const val MIN_SEARCH_LENGTH = 3

        // `in` takes a bounded list; ten keeps it well inside every SDK's limit.
        private const val UID_QUERY_CHUNK = 10
        private const val SEARCH_LIMIT = 20
    }
}
