package com.geoviksoft.turnia.core.data.datasource.firestore

import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.PendingWrite
import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackData
import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackWrite
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.UsernameDocument
import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.core.data.user.mappers.UsernameErrorMapper
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.domain.model.UsernameError
import com.geoviksoft.turnia.core.domain.username.USERNAME_SEARCH_MIN_LENGTH
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.outcomeCatching
import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.WriteBatch

/** A search hit: who it points at, and how old their profile is allowed to be in the cache. */
data class UsernameMatch(
    val uid: UserId,
    val username: String,
    val updateAt: BaseTimestamp?,
)

/**
 * Interacts with Firestore: `usernames/{username}`
 */
class UsernameFirestore(
    private val firestore: FirebaseFirestore,
    private val errorMapper: UsernameErrorMapper,
) {

    suspend fun claim(username: String, uid: UserId): Outcome<Unit, UsernameError> =
        outcomeCatching(TAG, { throwable -> errorMapper.mapClaim(throwable, username) }) {
            Logger.i(TAG, "Claim username")
            reservation(username).set(UsernameDocument(username = username, uid = uid.value))
            trackWrite(TAG, "claim")
        }

    fun touch(batch: WriteBatch, username: String): PendingWrite {
        Logger.d(TAG, "Touch the username marker")
        batch.set(
            reservation(username),
            mapOf(UsernameDocument.FIELD_UPDATE_AT to FieldValue.serverTimestamp),
            merge = true,
        )

        return PendingWrite { trackWrite(TAG, "touch") }
    }

    suspend fun search(
        prefix: String,
        limit: Int = SEARCH_LIMIT
    ): Outcome<List<UsernameMatch>, UsernameError> =
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

            snapshot.documents.map { document ->
                document.data(UsernameDocument.serializer()).let {
                    UsernameMatch(
                        uid = UserId(it.uid),
                        username = it.username,
                        updateAt = it.updateAt,
                    )
                }
            }
        }

    private fun reservation(username: String) =
        firestore.collection(PATH_USERNAMES).document(username)

    companion object {
        private const val TAG = "UsernameFirestore"
        private const val PATH_USERNAMES = "usernames"

        private const val SEARCH_LIMIT = 20
    }
}
