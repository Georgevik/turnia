package com.geoviksoft.turnia.core.data.datasource.firestore

import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackData
import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackWrite
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.UsernameDocument
import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.core.data.user.mappers.UserDocumentMapper
import com.geoviksoft.turnia.core.data.user.mappers.UsernameErrorMapper
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.domain.model.UserProfile
import com.geoviksoft.turnia.core.domain.model.UsernameError
import com.geoviksoft.turnia.core.domain.username.USERNAME_SEARCH_MIN_LENGTH
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.outcomeCatching
import dev.gitlive.firebase.firestore.FirebaseFirestore

/**
 * Interacts with Firestore: `usernames/{username}`
 */
class UsernameFirestore(
    private val firestore: FirebaseFirestore,
    private val userDocumentMapper: UserDocumentMapper,
    private val errorMapper: UsernameErrorMapper,
) {

    suspend fun claim(
        username: String,
        uid: UserId,
        name: String
    ): Outcome<Unit, UsernameError> =
        outcomeCatching(TAG, { throwable -> errorMapper.mapClaim(throwable, username) }) {
            Logger.i(TAG, "Claim username")
            firestore.collection(PATH_USERNAMES).document(username)
                .set(UsernameDocument(username = username, uid = uid.value, name = name))
            trackWrite(TAG, "claim")
        }

    suspend fun findByUids(uids: List<UserId>): Outcome<List<UserProfile>, UsernameError> =
        outcomeCatching(TAG, { UsernameError.SaveFailed }) {
            uids.chunked(UID_QUERY_CHUNK).flatMap { chunk ->
                val snapshot = firestore.collection(PATH_USERNAMES)
                    .where { UsernameDocument.FIELD_UID inArray chunk.map { it.value } }
                    .get().trackData(TAG, "findByUids")
                Logger.d(TAG, "Resolved ${snapshot.documents.size} of ${chunk.size} uids")

                snapshot.documents.map { it.data(UsernameDocument.serializer()).let(userDocumentMapper::map) }
            }
        }

    suspend fun search(
        prefix: String,
        limit: Int = SEARCH_LIMIT
    ): Outcome<List<UserProfile>, UsernameError> =
        outcomeCatching(TAG, { UsernameError.SaveFailed }) {
            if (prefix.length < USERNAME_SEARCH_MIN_LENGTH) return@outcomeCatching emptyList()

            val snapshot = firestore.collection(PATH_USERNAMES)
                .where {
                    (UsernameDocument.FIELD_USERNAME greaterThanOrEqualTo prefix) and
                            (UsernameDocument.FIELD_USERNAME lessThanOrEqualTo prefix + '￿')
                }
                .limit(limit)
                .get().trackData(TAG, "search")
            Logger.d(TAG, "Username search '$prefix': ${snapshot.documents.size} results")

            snapshot.documents.map { it.data(UsernameDocument.serializer()).let(userDocumentMapper::map) }
        }

    companion object {
        private const val TAG = "UsernameFirestore"
        private const val PATH_USERNAMES = "usernames"

        // `in` takes a bounded list; ten keeps it well inside every SDK's limit.
        private const val UID_QUERY_CHUNK = 10
        private const val SEARCH_LIMIT = 20
    }
}
