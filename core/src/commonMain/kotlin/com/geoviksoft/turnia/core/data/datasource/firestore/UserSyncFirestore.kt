package com.geoviksoft.turnia.core.data.datasource.firestore

import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.PendingWrite
import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackWrite
import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackedSnapshots
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.EventSyncUpdateAt
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.UserSyncDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.errors.GenericFirestoreError
import com.geoviksoft.turnia.core.data.datasource.firestore.sync.SharedListeners
import com.geoviksoft.turnia.core.data.datasource.firestore.sync.Synced
import com.geoviksoft.turnia.core.data.datasource.firestore.sync.awaitConfirmed
import com.geoviksoft.turnia.core.data.datasource.firestore.sync.isConfirmed
import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.UserId
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
import kotlin.time.Duration.Companion.seconds

/**
 * Interacts with Firestore: `users/{uid}/sync/updates`
 *
 * Every datasource asks this document whether its cache is behind, so opening a calendar can ask
 * several times over. All of them are answered by one shared listener.
 */
class UserSyncFirestore(
    private val firestore: FirebaseFirestore,
    scope: CoroutineScope,
) {

    private val listeners =
        SharedListeners<UserId, Synced<UserSyncDocument>>(scope, keepAlive = 10.minutes)

    // Apart from [listeners] so the audit tells what a colleague's calendar costs from what the
    // user's own sync does. A short keep-alive: it only has to ride out a quick back-and-forth, and
    // every minute it lingers after the calendar is gone is a read for each change nobody sees.
    private val sharedListeners =
        SharedListeners<UserId, Synced<UserSyncDocument?>>(scope, keepAlive = 30.seconds)

    fun observe(uid: UserId): Flow<UserSyncDocument> =
        synced(uid).map { it.value }.distinctUntilChanged()

    fun observeWithPendingWrites(uid: UserId): Flow<Synced<UserSyncDocument>> = synced(uid)

    /** The document as the server last confirmed it, served by the listener rather than a read. */
    suspend fun get(uid: UserId): Outcome<UserSyncDocument, GenericFirestoreError> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) { synced(uid).awaitConfirmed() }

    /**
     * Another user's markers, for a calendar they share with this one: what tells the viewer's cache
     * of that calendar it is behind. A null value is a listener that failed — above all a grant that
     * was withdrawn, which the rules answer by refusing the read — and unlike the user's own, it must
     * not pass for "nothing changed", or the viewer would keep the calendar they lost.
     */
    fun observeShared(ownerId: UserId): Flow<Synced<UserSyncDocument?>> =
        sharedListeners.shared(ownerId) {
            snapshots(ownerId, "sharedSync(snapshots)")
                .map { Synced<UserSyncDocument?>(it.value, it.confirmed) }
                .catch { throwable ->
                    Logger.e(TAG, "Shared sync updates listener failed", throwable)
                    emit(Synced(null, confirmed = true))
                }
        }

    private fun synced(uid: UserId): Flow<Synced<UserSyncDocument>> =
        listeners.shared(uid) {
            snapshots(uid, "sync(snapshots)").catch { throwable ->
                Logger.e(TAG, "Sync updates listener failed", throwable)
                emit(Synced(UserSyncDocument(), confirmed = true))
            }
        }

    private fun snapshots(uid: UserId, operation: String): Flow<Synced<UserSyncDocument>> =
        syncDocument(uid).trackedSnapshots(TAG, operation)
            .map { snapshot ->
                // A user who has never written anything has no sync document: nothing to catch up with.
                val document = if (!snapshot.exists) UserSyncDocument()
                else snapshot.data(UserSyncDocument.serializer())
                Synced(document, snapshot.metadata.isConfirmed)
            }
            .distinctUntilChanged()

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

    fun writePersonalEvents(batch: WriteBatch, uid: UserId, months: Set<YearMonth>) = write(
        "writePersonalEvents",
        batch,
        uid,
        UserSyncDocument(
            personalEventsUpdatedAt = months.associateWith {
                EventSyncUpdateAt(Timestamp.ServerTimestamp)
            },
        ),
    )

    fun writeGroupEventExtras(batch: WriteBatch, uid: UserId, months: Set<YearMonth>) = write(
        "writeGroupEventExtras",
        batch,
        uid,
        UserSyncDocument(
            groupEventExtrasUpdatedAt = months.associateWith {
                EventSyncUpdateAt(Timestamp.ServerTimestamp)
            },
        ),
    )

    fun writePersonalOneOffEvents(batch: WriteBatch, uid: UserId, months: Set<YearMonth>) = write(
        "writePersonalOneOffEvents",
        batch,
        uid,
        UserSyncDocument(
            personalOneOffEventsUpdatedAt = months.associateWith {
                EventSyncUpdateAt(Timestamp.ServerTimestamp)
            }
        ),
    )

    fun writePersonalEventTypes(batch: WriteBatch, uid: UserId) = write(
        "writePersonalEventTypes",
        batch,
        uid,
        UserSyncDocument(personalEventTypesUpdatedAt = Timestamp.ServerTimestamp),
    )

    fun writeAccount(batch: WriteBatch, uid: UserId) = write(
        "writeAccount",
        batch,
        uid,
        UserSyncDocument(accountUpdatedAt = Timestamp.ServerTimestamp),
    )

    fun writeProfile(batch: WriteBatch, uid: UserId) = write(
        "writeProfile",
        batch,
        uid,
        UserSyncDocument(profileUpdatedAt = Timestamp.ServerTimestamp),
    )

    /** A group this user just created, in the commit that creates it. */
    fun writeGroupJoined(batch: WriteBatch, uid: UserId, groupId: GroupId) = write(
        "writeGroupJoined",
        batch,
        uid,
        UserSyncDocument(groups = mapOf(groupId.value to Timestamp.ServerTimestamp)),
    )

    /** Copies a membership the server has just confirmed into [UserSyncDocument.groups], once. */
    suspend fun indexGroups(uid: UserId, groupIds: List<GroupId>) {
        val batch = firestore.batch()
        val syncWrite = write(
            "indexGroups",
            batch,
            uid,
            UserSyncDocument(
                groups = groupIds.associate { it.value to Timestamp.ServerTimestamp },
                groupsIndexed = true,
            ),
        )
        batch.commit()
        syncWrite.committed()
    }

    fun writeJoinRequests(batch: WriteBatch, uid: UserId) = write(
        "writeJoinRequests",
        batch,
        uid,
        UserSyncDocument(joinRequestsUpdatedAt = Timestamp.ServerTimestamp),
    )

    fun writePreferences(batch: WriteBatch, uid: UserId) = write(
        "writePreferences",
        batch,
        uid,
        UserSyncDocument(preferencesUpdatedAt = Timestamp.ServerTimestamp),
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

        return PendingWrite { trackWrite(TAG, operation) }
    }

    private fun syncDocument(uid: UserId) =
        firestore.collection(PATH_SYNC(uid.value)).document(DOCUMENT_UPDATES)

    companion object {
        private const val TAG = "UserSyncFirestore"
        private const val DOCUMENT_UPDATES = "updates"
        private fun PATH_SYNC(uid: String) = "users/${uid}/sync"
    }
}
