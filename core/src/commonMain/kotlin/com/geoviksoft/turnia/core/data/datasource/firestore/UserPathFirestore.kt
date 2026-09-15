package com.geoviksoft.turnia.core.data.datasource.firestore

import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackData
import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackWrite
import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackedSnapshots
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
import com.geoviksoft.turnia.core.system.valueOrNull
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
    private val userSyncFirestore: UserSyncFirestore,
    scope: CoroutineScope
) {
    // Held long past the screen that asks for it: a listener bills its result set again on every
    // re-attach, and nothing while it stays attached and nobody grants a calendar.
    private val calendarSharedWithMe =
        SharedListeners<UserId, CalendarsSharedWithMe>(scope, keepAlive = 30.minutes)

    /** The signed-in user's own profile. */
    suspend fun fetch(uid: UserId): Outcome<UserProfile, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            val document = ownDocument(uid) ?: return Outcome.Failure(UserProfileError.NotFound)
            mapper.map(uid, document)
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

    /** The signed-in user's own document, for what the profile model leaves out. */
    suspend fun getUserDocument(uid: UserId): Outcome<UserDocument, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            ownDocument(uid) ?: return Outcome.Failure(UserProfileError.NotFound)
        }

    suspend fun grantCalendarAccess(
        uid: UserId,
        granteeUid: UserId
    ): Outcome<Unit, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            Logger.i(TAG, "Grant calendar access")
            val batch = firestore.batch()
            batch.updateFields(queryUserDocument(uid)) {
                UserDocument.FIELD_CALENDAR_SHARED_WITH to FieldValue.arrayUnion(granteeUid.value)
                UserDocument.FIELD_UPDATE_AT to FieldValue.serverTimestamp
            }
            val syncWrite = userSyncFirestore.writeProfile(batch, uid)
            batch.commit()
            trackWrite(TAG, "grantCalendarAccess")
            syncWrite.committed()
        }

    suspend fun revokeCalendarAccess(
        uid: UserId,
        granteeUid: UserId
    ): Outcome<Unit, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            Logger.i(TAG, "Revoke calendar access")
            val batch = firestore.batch()
            batch.updateFields(queryUserDocument(uid)) {
                UserDocument.FIELD_CALENDAR_SHARED_WITH to FieldValue.arrayRemove(granteeUid.value)
                UserDocument.FIELD_UPDATE_AT to FieldValue.serverTimestamp
            }
            val syncWrite = userSyncFirestore.writeProfile(batch, uid)
            batch.commit()
            trackWrite(TAG, "revokeCalendarAccess")
            syncWrite.committed()
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
            val syncWrite = userSyncFirestore.writeProfile(batch, uid)
            batch.commit()
            trackWrite(TAG, "updateAvatar")
            markerWrite?.committed()
            syncWrite.committed()
        }

    /** Moves the markers like any profile write, so another device sees the flag without a stale cache. */
    suspend fun enableShowAds(uid: UserId, username: String): Outcome<Unit, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            Logger.i(TAG, "Enable show ads")

            val batch = firestore.batch()
            batch.set(
                queryUserDocument(uid),
                mapOf(
                    UserDocument.FIELD_SHOW_ADS to true,
                    UserDocument.FIELD_UPDATE_AT to FieldValue.serverTimestamp,
                ),
                merge = true,
            )
            val markerWrite = username.takeIf { it.isNotBlank() }
                ?.let { remoteUsernames.touch(batch, it) }
            val syncWrite = userSyncFirestore.writeProfile(batch, uid)
            batch.commit()
            trackWrite(TAG, "enableShowAds")
            markerWrite?.committed()
            syncWrite.committed()
        }

    suspend fun updateUsername(uid: UserId, username: String): Outcome<Unit, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            Logger.i(TAG, "Update username")
            val batch = firestore.batch()
            batch.updateFields(queryUserDocument(uid)) {
                UserDocument.FIELD_USERNAME to username
                UserDocument.FIELD_UPDATE_AT to FieldValue.serverTimestamp
            }
            val syncWrite = userSyncFirestore.writeProfile(batch, uid)
            batch.commit()
            trackWrite(TAG, "updateUsername")
            syncWrite.committed()
        }

    suspend fun update(uid: UserId, userPatched: UserDocument): Outcome<Unit, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            Logger.i(TAG, "Update user document")
            val batch = firestore.batch()
            batch.set(queryUserDocument(uid), userPatched)
            val syncWrite = userSyncFirestore.writeProfile(batch, uid)
            batch.commit()
            trackWrite(TAG, "updateUserDoc")
            syncWrite.committed()
        }

    /**
     * From the cache while the `profile` marker says it is current. Only this user's writes move it
     * — from here, another of their devices, or `updateProfile` — so a session start and the People
     * tab no longer each pay a read for a document that almost never changes. With no marker yet
     * the cached copy stands: the marker is written from this version on.
     */
    private suspend fun ownDocument(uid: UserId): UserDocument? {
        val cached = cachedProfile(uid)
        val marker = userSyncFirestore.get(uid).valueOrNull()?.profileUpdatedAt.toInstantOrNull()
        val cachedAt = cached?.updateAt.toInstantOrNull()

        if (cached != null && (marker == null || (cachedAt != null && cachedAt >= marker))) {
            Logger.d(TAG, "Own profile is settled, no read")
            return cached
        }
        return serverProfile(uid)
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
