package com.georgevik.turnia.core.data.datasource.firestore

import com.georgevik.turnia.core.data.datasource.firestore.analytics.trackData
import com.georgevik.turnia.core.data.datasource.firestore.analytics.trackWrite
import com.georgevik.turnia.core.data.datasource.firestore.doc.EventSyncUpdateAt
import com.georgevik.turnia.core.data.datasource.firestore.doc.UserSyncDocument
import com.georgevik.turnia.core.data.datasource.firestore.errors.GenericFirestoreError
import com.georgevik.turnia.core.data.datasource.firestore.sync.DebouncedReads
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.outcomeCatching
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.WriteBatch
import kotlinx.datetime.YearMonth
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Interacts with Firestore: `users/{uid}/sync/updates`
 *
 * Every datasource asks this document whether its cache is behind, so opening a calendar can ask
 * several times over. [debounce] serves the last answer for that long instead of paying a read per
 * question: the window bounds how stale a "your cache is fine" verdict can be.
 */
class UserSyncFirestore(
    private val firestore: FirebaseFirestore,
    private val debounce: Duration = DEFAULT_DEBOUNCE,
) {

    private val recentReads = DebouncedReads<UserId, UserSyncDocument>(debounce)

    suspend fun get(uid: UserId): Outcome<UserSyncDocument, GenericFirestoreError> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) {
            recentReads.cached(uid)?.let {
                Logger.d(TAG, "Sync updates within the debounce window, no read")
                return@outcomeCatching it
            }

            fetch(uid)
        }

    /**
     * Adds the marker to [batch] instead of writing it on its own, so it lands in the same commit
     * as the document it marks. A batch resolves every `ServerTimestamp` in it to a single commit
     * time, and that is what lets a reader see document and marker as equally old — written apart,
     * the marker is always the later of the two and the cache never looks current.
     */
    fun writePersonalEvents(batch: WriteBatch, uid: UserId, yearMonth: YearMonth) = write(
        batch,
        uid,
        UserSyncDocument(
            personalEventsUpdatedAt = mapOf(
                yearMonth to EventSyncUpdateAt(Timestamp.ServerTimestamp)
            ),
        ),
    )

    fun writePersonalEventTypes(batch: WriteBatch, uid: UserId) =
        write(batch, uid, UserSyncDocument(personalEventTypesUpdatedAt = Timestamp.ServerTimestamp))

    private fun write(batch: WriteBatch, uid: UserId, patch: UserSyncDocument) {
        Logger.d(TAG, "Update sync updates")
        // Without defaults, so the fields the patch does not carry are not encoded at all.
        batch.set(syncDocument(uid), patch, merge = true) { encodeDefaults = false }
        trackWrite(TAG)
        recentReads.forget(uid)
    }

    private suspend fun fetch(uid: UserId): UserSyncDocument {
        val snapshot = syncDocument(uid).get().trackData(TAG)
        Logger.d(TAG, "Sync updates. Cached: ${snapshot.metadata.isFromCache}")

        // A user who has never written anything has no sync document: nothing to catch up with.
        val document = if (!snapshot.exists) UserSyncDocument()
        else snapshot.data(UserSyncDocument.serializer())

        recentReads.remember(uid, document)
        return document
    }

    private fun syncDocument(uid: UserId) =
        firestore.collection(PATH_SYNC(uid.value)).document(DOCUMENT_UPDATES)

    companion object {
        private const val TAG = "UserSyncFirestore"
        private const val DOCUMENT_UPDATES = "updates"
        private fun PATH_SYNC(uid: String) = "users/${uid}/sync"

        val DEFAULT_DEBOUNCE: Duration = 10.seconds
    }
}
