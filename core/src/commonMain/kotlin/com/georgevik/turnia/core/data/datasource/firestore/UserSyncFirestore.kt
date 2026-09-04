package com.georgevik.turnia.core.data.datasource.firestore

import com.georgevik.turnia.core.data.datasource.firestore.doc.EventSyncUpdateAt
import com.georgevik.turnia.core.data.datasource.firestore.doc.UserSyncDocument
import com.georgevik.turnia.core.data.datasource.firestore.errors.GenericFirestoreError
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.outcomeCatching
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.datetime.YearMonth

/**
 * Interacts with Firestore: `users/{uid}/sync/updates`
 */
class UserSyncFirestore(
    private val firestore: FirebaseFirestore
) {

    suspend fun get(uid: UserId): Outcome<UserSyncDocument, GenericFirestoreError> =
        outcomeCatching(TAG,{ GenericFirestoreError(it) }) {
            val snapshot = syncDocument(uid).get()
            Logger.d(TAG, "Sync updates. Cached: ${snapshot.metadata.isFromCache}")

            // A user who has never written anything has no sync document: nothing to catch up with.
            if (!snapshot.exists) UserSyncDocument()
            else snapshot.data(UserSyncDocument.serializer())
        }

    suspend fun updatePersonalEvents(
        uid: UserId,
        yearMonth: YearMonth
    ): Outcome<Unit, GenericFirestoreError> =
        outcomeCatching(TAG,{ GenericFirestoreError(it) }) {
            val snapshot = syncDocument(uid).get()

            val syncDoc = if (!snapshot.exists) UserSyncDocument()
            else snapshot.data(UserSyncDocument.serializer())

            val eventSyncList = syncDoc.personalEventsUpdatedAt.toMutableMap().apply {
                this[yearMonth] = EventSyncUpdateAt(Timestamp.ServerTimestamp)
            }

            Logger.d(TAG, "Personal event sync updated")
            syncDocument(uid).set(syncDoc.copy(personalEventsUpdatedAt = eventSyncList))
        }


    suspend fun updatePersonalEventTypes(uid: UserId): Outcome<Unit, GenericFirestoreError> =
        update(uid, UserSyncDocument(personalEventTypesUpdatedAt = Timestamp.ServerTimestamp))

    private suspend fun update(
        uid: UserId,
        patch: UserSyncDocument
    ): Outcome<Unit, GenericFirestoreError> =
        outcomeCatching(TAG,{ GenericFirestoreError(it) }) {
            Logger.d(TAG, "Update sync updates")
            // Merging without defaults leaves the timestamps this patch does not carry untouched.
            syncDocument(uid).set(patch, merge = true) { encodeDefaults = false }
        }

    private fun syncDocument(uid: UserId) =
        firestore.collection(PATH_SYNC(uid.value)).document(DOCUMENT_UPDATES)

    companion object {
        private const val TAG = "UserSyncFirestore"
        private const val DOCUMENT_UPDATES = "updates"
        private fun PATH_SYNC(uid: String) = "users/${uid}/sync"
    }
}
