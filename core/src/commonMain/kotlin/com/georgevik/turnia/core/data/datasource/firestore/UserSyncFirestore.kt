package com.georgevik.turnia.core.data.datasource.firestore

import com.georgevik.turnia.core.data.datasource.firestore.analytics.PendingWrite
import com.georgevik.turnia.core.data.datasource.firestore.analytics.trackData
import com.georgevik.turnia.core.data.datasource.firestore.analytics.trackWrite
import com.georgevik.turnia.core.data.datasource.firestore.doc.EventSyncUpdateAt
import com.georgevik.turnia.core.data.datasource.firestore.doc.UserSyncDocument
import com.georgevik.turnia.core.data.datasource.firestore.errors.GenericFirestoreError
import com.georgevik.turnia.core.data.datasource.firestore.sync.DebouncedReads
import com.georgevik.turnia.core.data.datasource.firestore.sync.SharedListeners
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.outcomeCatching
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.WriteBatch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.datetime.YearMonth
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
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
    scope: CoroutineScope,
    private val debounce: Duration = DEFAULT_DEBOUNCE,
) {

    private val recentReads = DebouncedReads<UserId, UserSyncDocument>(debounce)
    private val listeners = SharedListeners<UserId, UserSyncDocument>(scope, keepAlive = 10.minutes)

    fun observe(uid: UserId): Flow<UserSyncDocument> = listeners.shared(uid) { snapshots(uid) }

    private fun snapshots(uid: UserId): Flow<UserSyncDocument> = syncDocument(uid).snapshots
        .map { snapshot ->
            snapshot.trackData(TAG, "sync(snapshots)")
            if (!snapshot.exists) UserSyncDocument()
            else snapshot.data(UserSyncDocument.serializer())
        }
        .distinctUntilChanged()
        .catch { throwable ->
            Logger.e(TAG, "Sync updates listener failed", throwable)
            emit(UserSyncDocument())
        }

    suspend fun get(uid: UserId): Outcome<UserSyncDocument, GenericFirestoreError> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) {
            recentReads.cached(uid)?.let {
                Logger.d(TAG, "Sync updates within the debounce window, no read")
                return@outcomeCatching it
            }

            fetch(uid)
        }

    fun writePersonalEvents(batch: WriteBatch, uid: UserId, yearMonth: YearMonth) = write(
        "writePersonalEvents",
        batch,
        uid,
        UserSyncDocument(
            personalEventsUpdatedAt = mapOf(
                yearMonth to EventSyncUpdateAt(Timestamp.ServerTimestamp)
            ),
        ),
    )

    fun writePersonalEventTypes(batch: WriteBatch, uid: UserId) = write(
        "writePersonalEventTypes",
        batch,
        uid,
        UserSyncDocument(personalEventTypesUpdatedAt = Timestamp.ServerTimestamp),
    )

    fun writePrivate(batch: WriteBatch, uid: UserId) = write(
        "writePrivate",
        batch,
        uid,
        UserSyncDocument(privateUpdatedAt = Timestamp.ServerTimestamp),
    )

    private fun write(
        operation: String,
        batch: WriteBatch,
        uid: UserId,
        patch: UserSyncDocument,
    ): PendingWrite {
        Logger.d(TAG, "Update sync updates")
        // Without defaults, so the fields the patch does not carry are not encoded at all.
        batch.set(syncDocument(uid), patch, merge = true) { encodeDefaults = false }
        recentReads.forget(uid)

        return PendingWrite { trackWrite(TAG, operation) }
    }

    private suspend fun fetch(uid: UserId): UserSyncDocument {
        val snapshot = syncDocument(uid).get().trackData(TAG, "sync")
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
