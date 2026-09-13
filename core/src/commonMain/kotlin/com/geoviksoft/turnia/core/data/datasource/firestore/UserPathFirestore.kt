package com.geoviksoft.turnia.core.data.datasource.firestore

import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackData
import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackedSnapshots
import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackWrite
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.UserDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.errors.UserProfileError
import com.geoviksoft.turnia.core.data.datasource.firestore.sync.SharedListeners
import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.core.data.user.mappers.UserDocumentMapper
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.domain.model.UserProfile
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.outcomeCatching
import com.geoviksoft.turnia.core.system.toFailure
import com.geoviksoft.turnia.core.system.toInstantOrNull
import com.geoviksoft.turnia.core.system.toSuccess
import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.time.Duration.Companion.minutes

private typealias CalendarsSharedWithMe = Outcome<List<UserProfile>, UserProfileError>

/**
 * Interacts with Firestore: `users/{uid}`
 */
class UserPathFirestore(
    private val firestore: FirebaseFirestore,
    private val mapper: UserDocumentMapper,
    private val remoteUsernames: UsernameFirestore,
    scope: CoroutineScope
) {
    // Held long past the screen that asks for it: a listener bills its result set again on every
    // re-attach, and nothing while it stays attached and nobody grants a calendar.
    private val calendarSharedWithMe =
        SharedListeners<UserId, CalendarsSharedWithMe>(scope, keepAlive = 30.minutes)

    suspend fun fetch(uid: UserId): Outcome<UserProfile, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            val snapshot = queryUserDocument(uid).get().trackData(TAG, "fetchProfile")
            Logger.d(TAG, "Fetch user from cache: ${snapshot.metadata.isFromCache}")

            if (!snapshot.exists) return Outcome.Failure(UserProfileError.NotFound)
            mapper.map(snapshot)
        }

    /**
     * Somebody else's profile, from the cache whenever [marker] says the cached copy is current.
     */
    suspend fun fetchProfile(
        uid: UserId,
        marker: BaseTimestamp?,
    ): Outcome<UserProfile, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            cachedProfile(uid)?.takeIf { it.isSettledAgainst(marker) }?.let { cached ->
                return@outcomeCatching mapper.map(uid, cached)
            }

            val document = serverProfile(uid)
                ?: return Outcome.Failure(UserProfileError.NotFound)
            mapper.map(uid, document)
        }

    /**
     * Profiles for a known list of uids, cache first.
     */
    suspend fun fetchProfiles(
        uids: List<UserId>,
        refresh: Boolean = false,
    ): Outcome<List<UserProfile>, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            uids.mapNotNull { uid ->
                val document = (if (refresh) null else cachedProfile(uid)) ?: serverProfile(uid)
                document?.let { mapper.map(uid, it) }
            }
        }

    fun fetchCalendarsSharedWithMe(uid: UserId): Flow<CalendarsSharedWithMe> =
        calendarSharedWithMe.shared(uid) { queryCalendarSharedWith(uid) }

    suspend fun getUserDocument(uid: UserId): Outcome<UserDocument, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            val snapshot = queryUserDocument(uid).get().trackData(TAG, "userDoc")
            if (!snapshot.exists) return Outcome.Failure(UserProfileError.NotFound)

            snapshot.data(UserDocument.serializer())
        }

    suspend fun grantCalendarAccess(
        uid: UserId,
        granteeUid: UserId
    ): Outcome<Unit, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            Logger.i(TAG, "Grant calendar access")
            queryUserDocument(uid).updateFields {
                UserDocument.FIELD_CALENDAR_SHARED_WITH to FieldValue.arrayUnion(granteeUid.value)
            }
            trackWrite(TAG, "grantCalendarAccess")
        }

    suspend fun revokeCalendarAccess(
        uid: UserId,
        granteeUid: UserId
    ): Outcome<Unit, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            Logger.i(TAG, "Revoke calendar access")
            queryUserDocument(uid).updateFields {
                UserDocument.FIELD_CALENDAR_SHARED_WITH to FieldValue.arrayRemove(granteeUid.value)
            }
            trackWrite(TAG, "revokeCalendarAccess")
        }

    /**
     * The avatar, and the marker that tells everyone else their cached copy is stale.
     */
    suspend fun updateAvatar(
        uid: UserId,
        username: String,
        animalIconId: String?,
        backgroundColor: String?,
    ): Outcome<Unit, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            Logger.i(TAG, "Update avatar")

            val batch = firestore.batch()
            batch.set(
                queryUserDocument(uid),
                mapOf(
                    UserDocument.FIELD_ANIMAL_ICON_ID to animalIconId,
                    UserDocument.FIELD_BACKGROUND_COLOR to backgroundColor,
                    UserDocument.FIELD_UPDATE_AT to FieldValue.serverTimestamp,
                ),
                merge = true,
            )
            // A user with no reservation yet has no marker to move, and `touch` would land on the
            // reservation's `create` rule, which this payload cannot satisfy.
            val markerWrite = username.takeIf { it.isNotBlank() }
                ?.let { remoteUsernames.touch(batch, it) }
            batch.commit()
            trackWrite(TAG, "updateAvatar")
            markerWrite?.committed()
        }

    suspend fun updateUsername(uid: UserId, username: String): Outcome<Unit, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            Logger.i(TAG, "Update username")
            queryUserDocument(uid).updateFields { UserDocument.FIELD_USERNAME to username }
            trackWrite(TAG, "updateUsername")
        }

    suspend fun update(uid: UserId, userPatched: UserDocument): Outcome<Unit, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            Logger.i(TAG, "Update user document")
            queryUserDocument(uid).set(userPatched)
            trackWrite(TAG, "updateUserDoc")
        }

    private suspend fun cachedProfile(uid: UserId): UserDocument? =
        queryUserDocument(uid).getCached(TAG, "profile(cache)")
            ?.data(UserDocument.serializer())

    private suspend fun serverProfile(uid: UserId): UserDocument? {
        val snapshot = queryUserDocument(uid).get(Source.SERVER).trackData(TAG, "profile(server)")
        return if (snapshot.exists) snapshot.data(UserDocument.serializer()) else null
    }

    private fun UserDocument.isSettledAgainst(marker: BaseTimestamp?): Boolean {
        val markerAt = marker.toInstantOrNull() ?: return false
        val cachedAt = updateAt.toInstantOrNull() ?: return false
        return cachedAt >= markerAt
    }

    private fun queryUserDocument(uid: UserId) =
        firestore.collection(PATH_USER).document(uid.value)

    private fun queryCalendarSharedWith(uid: UserId): Flow<CalendarsSharedWithMe> =
        firestore.collection(PATH_USER)
            .where { UserDocument.FIELD_CALENDAR_SHARED_WITH contains uid.value }
            .trackedSnapshots(TAG, "calendarsSharedWithMe(snapshots)")
            .map { snapshot ->
                snapshot.documents.map { mapper.map(it) }.toSuccess()
            }
            .distinctUntilChanged()
            .catch<CalendarsSharedWithMe> { throwable ->
                Logger.e(TAG, "Calendars shared with me listener failed", throwable)
                emit(UserProfileError.LoadFailed(throwable).toFailure())
            }

    companion object {
        private const val TAG = "UserPathFirestore"
        private const val PATH_USER = "users"
    }
}
