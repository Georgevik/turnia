package com.geoviksoft.turnia.core.data.datasource.firestore

import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.PendingWrite
import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackData
import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackedSnapshots
import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackWrite
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.EventSyncUpdateAt
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.GroupSyncDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.errors.GenericFirestoreError
import com.geoviksoft.turnia.core.data.datasource.firestore.sync.DebouncedReads
import com.geoviksoft.turnia.core.data.datasource.firestore.sync.SharedListeners
import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.outcomeCatching
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
 * Interacts with Firestore: `groups/{groupId}/sync/updates`
 */
class GroupSyncFirestore(
    private val firestore: FirebaseFirestore,
    scope: CoroutineScope,
    debounce: Duration = DEFAULT_DEBOUNCE,
) {

    private val recentReads = DebouncedReads<GroupId, GroupSyncDocument>(debounce)
    private val listeners = SharedListeners<GroupId, GroupSyncDocument>(scope, keepAlive = 10.minutes)

    fun observe(groupId: GroupId): Flow<GroupSyncDocument> =
        listeners.shared(groupId) { snapshots(groupId) }

    private fun snapshots(groupId: GroupId): Flow<GroupSyncDocument> =
        syncDocument(groupId).trackedSnapshots(TAG, "sync(snapshots)")
        .map { snapshot ->
            if (!snapshot.exists) GroupSyncDocument()
            else snapshot.data(GroupSyncDocument.serializer())
        }
        .distinctUntilChanged()
        .catch { throwable ->
            Logger.e(TAG, "Group sync listener failed", throwable)
            emit(GroupSyncDocument())
        }

    suspend fun get(groupId: GroupId): Outcome<GroupSyncDocument, GenericFirestoreError> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) {
            recentReads.cached(groupId)?.let {
                Logger.d(TAG, "Sync updates within the debounce window, no read")
                return@outcomeCatching it
            }

            val snapshot = syncDocument(groupId).get().trackData(TAG, "sync")
            Logger.d(TAG, "Sync updates. Cached: ${snapshot.metadata.isFromCache}")

            // A group nobody has written to has no sync document: nothing to catch up with.
            val document = if (!snapshot.exists) GroupSyncDocument()
            else snapshot.data(GroupSyncDocument.serializer())

            recentReads.remember(groupId, document)
            document
        }

    fun writeEvents(batch: WriteBatch, groupId: GroupId, yearMonth: YearMonth) = write(
        "writeEvents",
        batch,
        groupId,
        GroupSyncDocument(
            eventsUpdatedAt = mapOf(yearMonth to EventSyncUpdateAt(Timestamp.ServerTimestamp))
        ),
    )

    fun writeGroup(batch: WriteBatch, groupId: GroupId) =
        write("writeGroup", batch, groupId, GroupSyncDocument(groupUpdatedAt = Timestamp.ServerTimestamp))

    private fun write(
        operation: String,
        batch: WriteBatch,
        groupId: GroupId,
        patch: GroupSyncDocument,
    ): PendingWrite {
        Logger.d(TAG, "Update group sync updates")
        // Merging derives its field mask from the leaves, so only this marker is written.
        batch.set(syncDocument(groupId), patch, merge = true) { encodeDefaults = false }
        recentReads.forget(groupId)

        return PendingWrite { trackWrite(TAG, operation) }
    }

    private fun syncDocument(groupId: GroupId) =
        firestore.collection(PATH_SYNC(groupId.value)).document(DOCUMENT_UPDATES)

    companion object {
        private const val TAG = "GroupSyncFirestore"
        private const val DOCUMENT_UPDATES = "updates"
        private fun PATH_SYNC(groupId: String) = "groups/${groupId}/sync"

        val DEFAULT_DEBOUNCE: Duration = 10.seconds
    }
}
