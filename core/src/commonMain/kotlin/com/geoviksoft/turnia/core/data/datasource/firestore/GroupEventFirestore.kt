package com.geoviksoft.turnia.core.data.datasource.firestore

import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackData
import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackWrite
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.DocHolder
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.GroupEventDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.GroupSyncDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.errors.GenericFirestoreError
import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.core.domain.model.EventId
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.outcomeCatching
import com.geoviksoft.turnia.core.system.toInstantOrNull
import com.geoviksoft.turnia.core.system.toTimestamp
import com.geoviksoft.turnia.core.system.toYearMonth
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.YearMonthRange
import kotlinx.datetime.yearMonth
import kotlin.time.Instant

/**
 * Interacts with Firestore: `groups/{groupId}/events`
 */
class GroupEventFirestore(
    private val firestore: FirebaseFirestore,
    private val groupSyncFirestore: GroupSyncFirestore,
) {
    /**
     * [assigneeId] narrows the query to one person's events. A revoked user has to pass it: the
     * security rules only let them read the events assigned to them, and on a `list` Firestore
     * proves that from the query's own filters, so an unfiltered read is denied outright.
     */
    fun get(
        groupId: GroupId,
        from: Instant,
        until: Instant,
        assigneeId: UserId? = null,
    ): Flow<List<DocHolder<GroupEventDocument>>> =
        flow {
            val months = YearMonthRange(from.toYearMonth(), until.toYearMonth())
            val cachedEvents =
                queryEvents(groupId, months.associateWith { null }, Source.CACHE, assigneeId)

            cachedEvents.visible().takeIf { it.isNotEmpty() }?.let {
                emit(cachedEvents.visible())
            }

            var known = cachedEvents
            var settled = false

            emitAll(
                groupSyncFirestore.observe(groupId).mapNotNull { sync ->
                    val staleMonths = staleEventMonths(months, sync, known.updatedByMonth())

                    if (staleMonths.isEmpty()) {
                        if (settled) {
                            return@mapNotNull null
                        }

                        settled = true
                        return@mapNotNull known.visible()
                    }

                    val serverEvents = queryEvents(
                        groupId,
                        staleMonths.mapValues { (_, updatedAt) -> updatedAt?.toTimestamp() },
                        Source.SERVER,
                        assigneeId,
                    ).associateBy { it.id }.toMutableMap()

                    val merged = known.map { cached -> serverEvents.remove(cached.id) ?: cached }
                    known = merged + serverEvents.values
                    settled = true
                    known.visible()
                }
            )
        }.catch { throwable ->
            Logger.e(TAG, "Events listener failed", throwable)
            emit(emptyList())
        }

    suspend fun set(
        groupId: GroupId,
        eventId: EventId,
        event: GroupEventDocument
    ): Outcome<Unit, GenericFirestoreError> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) {
            Logger.d(TAG, "Set group event document")

            val batch = firestore.batch()
            batch.set(eventDocument(groupId, eventId), event)
            val syncWrite =
                groupSyncFirestore.writeEvents(batch, groupId, YearMonth.parse(event.yearMonth))
            batch.commit()
            trackWrite(TAG, "setEvent")
            syncWrite.committed()
        }

    suspend fun delete(
        groupId: GroupId,
        eventId: EventId,
        eventDate: LocalDate
    ): Outcome<Unit, GenericFirestoreError> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) {
            Logger.d(TAG, "Delete group event document")

            val batch = firestore.batch()
            batch.updateFields(eventDocument(groupId, eventId)) {
                GroupEventDocument.FIELD_IS_DELETED to true
                GroupEventDocument.FIELD_UPDATE_AT to Timestamp.ServerTimestamp
            }
            val syncWrite = groupSyncFirestore.writeEvents(batch, groupId, eventDate.yearMonth)
            batch.commit()
            trackWrite(TAG, "deleteEvent")
            syncWrite.committed()
        }

    /**
     * Offers a shift for swap, or withdraws it.
     */
    suspend fun updateOnSwap(
        groupId: GroupId,
        eventId: EventId,
        eventDate: LocalDate,
        onSwap: Boolean,
    ): Outcome<Unit, GenericFirestoreError> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) {
            Logger.d(TAG, "Set group event onSwap to $onSwap")

            val batch = firestore.batch()
            batch.updateFields(eventDocument(groupId, eventId)) {
                GroupEventDocument.FIELD_ON_SWAP to onSwap
                GroupEventDocument.FIELD_UPDATE_AT to Timestamp.ServerTimestamp
            }
            val syncWrite = groupSyncFirestore.writeEvents(batch, groupId, eventDate.yearMonth)
            batch.commit()
            trackWrite(TAG, "updateOnSwap")
            syncWrite.committed()
        }

    private suspend fun queryEvents(
        groupId: GroupId,
        months: Map<YearMonth, Timestamp?>,
        source: Source,
        assigneeId: UserId?,
    ): List<DocHolder<GroupEventDocument>> {
        if (months.isEmpty()) return emptyList()

        val snapshot = firestore.collection(PATH_EVENTS(groupId.value)).where {
            val clauses = months.map { (month, sinceUpdateAt) ->
                val inMonth = GroupEventDocument.FIELD_YEAR_MONTH equalTo month.toString()
                val changed =
                    sinceUpdateAt?.let { GroupEventDocument.FIELD_UPDATE_AT greaterThan it }

                if (changed == null) inMonth else inMonth and changed
            }

            val inMonths = any(*clauses.toTypedArray())
            val mine = assigneeId?.let { GroupEventDocument.FIELD_ASSIGNEE_ID equalTo it.value }

            when {
                mine == null -> inMonths
                inMonths == null -> mine
                else -> mine and inMonths
            }
        }.get(source).trackData(TAG, "events($source)")

        Logger.d(
            TAG,
            "Group events for (${months.keys.joinToString()}). Source: $source. " +
                    "Amount: ${snapshot.documents.size}"
        )
        return snapshot.documents.map {
            DocHolder(id = it.reference.id, doc = it.data(GroupEventDocument.serializer()))
        }
    }

    private fun List<DocHolder<GroupEventDocument>>.visible() = filterNot { it.doc.isDeleted }

    private fun List<DocHolder<GroupEventDocument>>.updatedByMonth(): Map<YearMonth, Instant> =
        groupBy { YearMonth.parse(it.doc.yearMonth) }
            .mapNotNull { (month, docs) ->
                docs.mapNotNull { it.doc.updateAt.toInstantOrNull() }.maxOrNull()
                    ?.let { month to it }
            }
            .toMap()

    /**
     * The months the server has moved on from, compared month by month: a local write in one month
     * would otherwise mask an older change another device made in a different one.
     */
    private fun staleEventMonths(
        months: YearMonthRange,
        sync: GroupSyncDocument,
        cacheUpdatedAt: Map<YearMonth, Instant>
    ): Map<YearMonth, Instant?> {
        val serverUpdatedAt = sync.eventsUpdatedAt

        // Each stale month keeps its own cursor: what this device already holds of *that* month.
        val stale = months.mapNotNull { month ->
            val server = serverUpdatedAt[month]?.updatedAt.toInstantOrNull()
                ?: return@mapNotNull null
            val cached = cacheUpdatedAt[month]

            if (cached == null || server > cached) month to cached else null
        }.toMap()

        Logger.d(TAG, "Stale months: ${stale.keys.joinToString().ifEmpty { "none" }}")
        return stale
    }

    private fun eventDocument(groupId: GroupId, eventId: EventId) =
        firestore.collection(PATH_EVENTS(groupId.value)).document(eventId.value)

    companion object {
        private const val TAG = "GroupEventFirestore"
        private fun PATH_EVENTS(groupId: String) = "groups/${groupId}/events"
    }
}
