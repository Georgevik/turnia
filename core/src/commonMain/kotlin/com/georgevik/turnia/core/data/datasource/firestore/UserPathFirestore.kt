package com.georgevik.turnia.core.data.datasource.firestore

import com.georgevik.turnia.core.data.datasource.firestore.analytics.trackData
import com.georgevik.turnia.core.data.datasource.firestore.analytics.trackWrite
import com.georgevik.turnia.core.data.datasource.firestore.doc.UserDocument
import com.georgevik.turnia.core.data.datasource.firestore.errors.UserProfileError
import com.georgevik.turnia.core.data.datasource.firestore.mappers.UserDocumentMapper
import com.georgevik.turnia.core.data.datasource.firestore.sync.SharedListeners
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.EventTypeId
import com.georgevik.turnia.core.domain.model.GroupId
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.domain.model.UserProfile
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.outcomeCatching
import com.georgevik.turnia.core.system.toFailure
import com.georgevik.turnia.core.system.toSuccess
import dev.gitlive.firebase.firestore.DocumentSnapshot
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
    scope: CoroutineScope
) {
    // Held long past the screen that asks for it: a listener bills its result set again on every
    // re-attach, and nothing while it stays attached and nobody grants a calendar.
    private val calendarSharedWithMe =
        SharedListeners<UserId, CalendarsSharedWithMe>(scope, keepAlive = 30.minutes)

    suspend fun fetch(uid: UserId): Outcome<UserProfile, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            val snapshot = firestore.collection(PATH_USER).document(uid.value).get().trackData(TAG)
            Logger.d(TAG, "Fetch user from cache: ${snapshot.metadata.isFromCache}")

            if (!snapshot.exists) return Outcome.Failure(UserProfileError.NotFound)
            mapper.map(snapshot)
        }

    fun fetchCalendarsSharedWithMe(uid: UserId): Flow<CalendarsSharedWithMe> =
        calendarSharedWithMe.shared(uid) { queryCalendarSharedWith(uid) }

    suspend fun getUserDocument(uid: UserId): Outcome<UserDocument, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            val snapshot = firestore.collection(PATH_USER).document(uid.value).get().trackData(TAG)
            if (!snapshot.exists) return Outcome.Failure(UserProfileError.NotFound)

            snapshot.data(UserDocument.serializer())
        }

    /**
     * The user's own document, from the cache whenever it is there.
     *
     * `users/{uid}` is only ever written by its owner, so a cached copy is this device's own last
     * word. The group type colours are read through here on every group screen, and asking the
     * server each time bought nothing but reads.
     */
    suspend fun getCachedUserDocument(uid: UserId): Outcome<UserDocument, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            cachedUserDocument(uid)?.let {
                return@outcomeCatching it.data(UserDocument.serializer())
            }

            val snapshot = queryUserDocument(uid).get(Source.SERVER).trackData(TAG)
            if (!snapshot.exists) return Outcome.Failure(UserProfileError.NotFound)

            snapshot.data(UserDocument.serializer())
        }

    /** A document the cache does not have makes the read fail rather than come back empty. */
    private suspend fun cachedUserDocument(uid: UserId): DocumentSnapshot? = try {
        queryUserDocument(uid).get(Source.CACHE).trackData(TAG).takeIf { it.exists }
    } catch (exception: Exception) {
        Logger.d(TAG, "The user document is not cached yet: ${exception.message}")
        null
    }

    suspend fun grantCalendarAccess(
        uid: UserId,
        granteeUid: UserId
    ): Outcome<Unit, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            Logger.i(TAG, "Grant calendar access")
            firestore.collection(PATH_USER).document(uid.value).updateFields {
                UserDocument.FIELD_CALENDAR_SHARED_WITH to FieldValue.arrayUnion(granteeUid.value)
            }
            trackWrite(TAG)
        }

    suspend fun revokeCalendarAccess(
        uid: UserId,
        granteeUid: UserId
    ): Outcome<Unit, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            Logger.i(TAG, "Revoke calendar access")
            firestore.collection(PATH_USER).document(uid.value).updateFields {
                UserDocument.FIELD_CALENDAR_SHARED_WITH to FieldValue.arrayRemove(granteeUid.value)
            }
            trackWrite(TAG)
        }

    suspend fun updateTypeColor(
        uid: UserId,
        groupId: GroupId,
        typeId: EventTypeId,
        color: String
    ): Outcome<Unit, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            Logger.i(TAG, "Update group event type colour")
            val key = UserDocument.typeColorKey(groupId.value, typeId.value)
            firestore.collection(PATH_USER).document(uid.value).updateFields {
                "${UserDocument.FIELD_TYPE_COLORS}.$key" to color
            }
            trackWrite(TAG)
        }

    suspend fun updateUsername(uid: UserId, username: String): Outcome<Unit, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            Logger.i(TAG, "Update username")
            firestore.collection(PATH_USER).document(uid.value).updateFields {
                UserDocument.FIELD_USERNAME to username
            }
            trackWrite(TAG)
        }

    suspend fun update(uid: UserId, userPatched: UserDocument): Outcome<Unit, UserProfileError> =
        outcomeCatching(TAG, { UserProfileError.LoadFailed(it) }) {
            Logger.i(TAG, "Update user document")
            firestore.collection(PATH_USER).document(uid.value).set(userPatched)
            trackWrite(TAG)
        }

    private fun queryUserDocument(uid: UserId) =
        firestore.collection(PATH_USER).document(uid.value)

    private fun queryCalendarSharedWith(uid: UserId): Flow<CalendarsSharedWithMe> =
        firestore.collection(PATH_USER)
            .where { UserDocument.FIELD_CALENDAR_SHARED_WITH contains uid.value }
            .snapshots
            .map { snapshot ->
                snapshot.trackData(TAG)
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
