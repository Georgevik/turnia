package com.geoviksoft.turnia.core.data.datasource.firestore

import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.PendingWrite
import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackedSnapshots
import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackWrite
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.EventSyncUpdateAt
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.GroupSyncDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.errors.GenericFirestoreError
import com.geoviksoft.turnia.core.data.datasource.firestore.sync.SharedListeners
import com.geoviksoft.turnia.core.data.datasource.firestore.sync.Synced
import com.geoviksoft.turnia.core.data.datasource.firestore.sync.awaitConfirmed
import com.geoviksoft.turnia.core.data.datasource.firestore.sync.isConfirmed
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
import kotlin.time.Duration.Companion.minutes

/**
 * Interacts with Firestore: `groups/{groupId}/sync/updates`
 */
class GroupSyncFirestore(
    private val firestore: FirebaseFirestore,
    scope: CoroutineScope,
) {

    private val listeners =
        SharedListeners<GroupId, Synced<GroupSyncDocument>>(scope, keepAlive = 10.minutes)

    fun observe(groupId: GroupId): Flow<GroupSyncDocument> =
        synced(groupId).map { it.value }.distinctUntilChanged()

    /** The document as the server last confirmed it, served by the listener rather than a read. */
    suspend fun get(groupId: GroupId): Outcome<GroupSyncDocument, GenericFirestoreError> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) { synced(groupId).awaitConfirmed() }

    private fun synced(groupId: GroupId): Flow<Synced<GroupSyncDocument>> =
        listeners.shared(groupId) { snapshots(groupId) }

    private fun snapshots(groupId: GroupId): Flow<Synced<GroupSyncDocument>> =
        syncDocument(groupId).trackedSnapshots(TAG, "sync(snapshots)")
            .map { snapshot ->
                // A group nobody has written to has no sync document: nothing to catch up with.
                val document = if (!snapshot.exists) GroupSyncDocument()
                else snapshot.data(GroupSyncDocument.serializer())
                Synced(document, snapshot.metadata.isConfirmed)
            }
            .distinctUntilChanged()
            .catch { throwable ->
                Logger.e(TAG, "Group sync listener failed", throwable)
                emit(Synced(GroupSyncDocument(), confirmed = true))
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

        return PendingWrite { trackWrite(TAG, operation) }
    }

    private fun syncDocument(groupId: GroupId) =
        firestore.collection(PATH_SYNC(groupId.value)).document(DOCUMENT_UPDATES)

    companion object {
        private const val TAG = "GroupSyncFirestore"
        private const val DOCUMENT_UPDATES = "updates"
        private fun PATH_SYNC(groupId: String) = "groups/${groupId}/sync"
    }
}
