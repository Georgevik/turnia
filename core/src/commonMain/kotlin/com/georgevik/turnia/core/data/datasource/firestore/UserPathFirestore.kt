package com.georgevik.turnia.core.data.datasource.firestore

import com.georgevik.turnia.core.data.datasource.firestore.analytics.trackData
import com.georgevik.turnia.core.data.datasource.firestore.analytics.trackWrite
import com.georgevik.turnia.core.data.datasource.firestore.doc.UserDocument
import com.georgevik.turnia.core.data.datasource.firestore.errors.UserProfileError
import com.georgevik.turnia.core.data.datasource.firestore.mappers.UserDocumentMapper
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.EventTypeId
import com.georgevik.turnia.core.domain.model.GroupId
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.domain.model.UserProfile
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.outcomeCatching
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore

/**
 * Interacts with Firestore: `users/{uid}`
 */
class UserPathFirestore(
    private val firestore: FirebaseFirestore,
    private val mapper: UserDocumentMapper,
) {

    suspend fun fetch(uid: UserId): Outcome<UserProfile, UserProfileError> =
        outcomeCatching(TAG,{ UserProfileError.LoadFailed(it) }) {
            val snapshot = firestore.collection(PATH_USER).document(uid.value).get().trackData(TAG)
            Logger.d(TAG, "Fetch user from cache: ${snapshot.metadata.isFromCache}")

            if (!snapshot.exists) return Outcome.Failure(UserProfileError.NotFound)
            mapper.map(snapshot)
        }

    suspend fun fetchCalendarsSharedWithMe(uid: UserId): Outcome<List<UserProfile>, UserProfileError> =
        outcomeCatching(TAG,{ UserProfileError.LoadFailed(it) }) {
            val snapshot = firestore.collection(PATH_USER).where {
                UserDocument.FIELD_CALENDAR_SHARED_WITH contains uid.value
            }.get().trackData(TAG)
            Logger.d(TAG, "Calendars shared with me: ${snapshot.documents.size}")

            snapshot.documents.map { mapper.map(it) }
        }

    suspend fun getUserDocument(uid: UserId): Outcome<UserDocument, UserProfileError> =
        outcomeCatching(TAG,{ UserProfileError.LoadFailed(it) }) {
            val snapshot = firestore.collection(PATH_USER).document(uid.value).get().trackData(TAG)
            if (!snapshot.exists) return Outcome.Failure(UserProfileError.NotFound)

            snapshot.data(UserDocument.serializer())
        }

    suspend fun grantCalendarAccess(
        uid: UserId,
        granteeUid: UserId
    ): Outcome<Unit, UserProfileError> =
        outcomeCatching(TAG,{ UserProfileError.LoadFailed(it) }) {
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
        outcomeCatching(TAG,{ UserProfileError.LoadFailed(it) }) {
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
        outcomeCatching(TAG,{ UserProfileError.LoadFailed(it) }) {
            Logger.i(TAG, "Update group event type colour")
            val key = UserDocument.typeColorKey(groupId.value, typeId.value)
            firestore.collection(PATH_USER).document(uid.value).updateFields {
                "${UserDocument.FIELD_TYPE_COLORS}.$key" to color
            }
            trackWrite(TAG)
        }

    suspend fun updateUsername(uid: UserId, username: String): Outcome<Unit, UserProfileError> =
        outcomeCatching(TAG,{ UserProfileError.LoadFailed(it) }) {
            Logger.i(TAG, "Update username")
            firestore.collection(PATH_USER).document(uid.value).updateFields {
                UserDocument.FIELD_USERNAME to username
            }
            trackWrite(TAG)
        }

    suspend fun update(uid: UserId, userPatched: UserDocument): Outcome<Unit, UserProfileError> =
        outcomeCatching(TAG,{ UserProfileError.LoadFailed(it) }) {
            Logger.i(TAG, "Update user document")
            firestore.collection(PATH_USER).document(uid.value).set(userPatched)
            trackWrite(TAG)
        }

    companion object {
        private const val TAG = "UserPathFirestore"
        private const val PATH_USER = "users"
    }
}
