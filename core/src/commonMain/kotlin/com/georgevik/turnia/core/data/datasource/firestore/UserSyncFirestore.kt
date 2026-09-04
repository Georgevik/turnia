package com.georgevik.turnia.core.data.datasource.firestore

import com.georgevik.turnia.core.data.datasource.firestore.analytics.trackData
import com.georgevik.turnia.core.data.datasource.firestore.analytics.trackWrite
import com.georgevik.turnia.core.data.datasource.firestore.doc.EventSyncUpdateAt
import com.georgevik.turnia.core.data.datasource.firestore.doc.UserSyncDocument
import com.georgevik.turnia.core.data.datasource.firestore.errors.GenericFirestoreError
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.outcomeCatching
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.WriteBatch
import kotlinx.datetime.YearMonth
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Interacts with Firestore: `users/{uid}/sync/updates`
 *
 * Every datasource asks this document whether its cache is behind, so opening a calendar can ask
 * several times over. [debounce] serves the last answer for that long instead of paying a read per
 * question: the window bounds how stale a "your cache is fine" verdict can be.
 */
@OptIn(ExperimentalAtomicApi::class)
class UserSyncFirestore(
    private val firestore: FirebaseFirestore,
    private val debounce: Duration = DEFAULT_DEBOUNCE,
) {

    private data class Cached(val document: UserSyncDocument, val readAt: Instant)

    // Immutable map swapped atomically: Firestore resolves its calls on its own threads.
    private val recentReads = AtomicReference(emptyMap<UserId, Cached>())

    suspend fun get(uid: UserId): Outcome<UserSyncDocument, GenericFirestoreError> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) {
            recentReads.load()[uid]
                ?.takeIf { Clock.System.now() - it.readAt < debounce }
                ?.let {
                    Logger.d(TAG, "Sync updates within the debounce window, no read")
                    return@outcomeCatching it.document
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
        forget(uid)
    }

    private suspend fun fetch(uid: UserId): UserSyncDocument {
        val snapshot = syncDocument(uid).get().trackData(TAG)
        Logger.d(TAG, "Sync updates. Cached: ${snapshot.metadata.isFromCache}")

        // A user who has never written anything has no sync document: nothing to catch up with.
        val document = if (!snapshot.exists) UserSyncDocument()
        else snapshot.data(UserSyncDocument.serializer())

        remember(uid, document)
        return document
    }

    private fun remember(uid: UserId, document: UserSyncDocument) {
        val cached = Cached(document, Clock.System.now())
        updateReads { it + (uid to cached) }
    }

    /** After a write the document is ours and different: the next question deserves a real read. */
    private fun forget(uid: UserId) = updateReads { it - uid }

    private fun updateReads(block: (Map<UserId, Cached>) -> Map<UserId, Cached>) {
        while (true) {
            val current = recentReads.load()
            if (recentReads.compareAndSet(current, block(current))) return
        }
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
