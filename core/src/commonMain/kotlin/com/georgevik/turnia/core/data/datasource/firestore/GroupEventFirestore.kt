package com.georgevik.turnia.core.data.datasource.firestore

import com.georgevik.turnia.core.data.datasource.firestore.analytics.trackData
import com.georgevik.turnia.core.data.datasource.firestore.analytics.trackWrite
import com.georgevik.turnia.core.data.datasource.firestore.doc.DocHolder
import com.georgevik.turnia.core.data.datasource.firestore.doc.GroupEventDocument
import com.georgevik.turnia.core.data.datasource.firestore.errors.GenericFirestoreError
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.EventId
import com.georgevik.turnia.core.domain.model.GroupId
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.outcomeCatching
import com.georgevik.turnia.core.system.toFailure
import com.georgevik.turnia.core.system.toInstantOrNull
import com.georgevik.turnia.core.system.toSuccess
import com.georgevik.turnia.core.system.toTimestamp
import com.georgevik.turnia.core.system.toYearMonth
import com.georgevik.turnia.core.system.valueOrNull
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
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


    fun get(
        groupId: GroupId,
        from: Instant,
        until: Instant
    ): Flow<Outcome<List<DocHolder<GroupEventDocument>>, GenericFirestoreError>> =
        flow<Outcome<List<DocHolder<GroupEventDocument>>, GenericFirestoreError>> {
            val months = YearMonthRange(from.toYearMonth(), until.toYearMonth())
            val cachedEvents = queryEvents(groupId, months.associateWith { null }, Source.CACHE)
            emit(cachedEvents.filterNot { it.doc.isDeleted }.toSuccess())

            val cacheUpdatedAt = cachedEvents.updatedByMonth()
            val staleMonths =
                if (cacheUpdatedAt.isEmpty()) months.associateWith { null }
                else staleEventMonths(groupId, months, cacheUpdatedAt)

            // Everything already in hand: the first emission was the answer.
            if (staleMonths.isEmpty()) return@flow

            val serverEvents = queryEvents(
                groupId,
                staleMonths.mapValues { (_, updatedAt) -> updatedAt?.toTimestamp() },
                Source.SERVER
            ).associateBy { it.id }.toMutableMap()

            val merged = cachedEvents.map { cached -> serverEvents.remove(cached.id) ?: cached }
            emit((merged + serverEvents.values).filterNot { it.doc.isDeleted }.toSuccess())
        }.catch { throwable ->
            Logger.e(TAG, "Failed to read group events", throwable)
            emit(GenericFirestoreError(throwable).toFailure())
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
            groupSyncFirestore.writeEvents(batch, groupId, YearMonth.parse(event.yearMonth))
            batch.commit()
            trackWrite(TAG)
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
            groupSyncFirestore.writeEvents(batch, groupId, eventDate.yearMonth)
            batch.commit()
            trackWrite(TAG)
        }

    private suspend fun queryEvents(
        groupId: GroupId,
        months: Map<YearMonth, Timestamp?>,
        source: Source
    ): List<DocHolder<GroupEventDocument>> {
        if (months.isEmpty()) return emptyList()

        val snapshot = firestore.collection(PATH_EVENTS(groupId.value)).where {
            val clauses = months.map { (month, sinceUpdateAt) ->
                val inMonth = GroupEventDocument.FIELD_YEAR_MONTH equalTo month.toString()
                val changed =
                    sinceUpdateAt?.let { GroupEventDocument.FIELD_UPDATE_AT greaterThan it }

                if (changed == null) inMonth else inMonth and changed
            }

            any(*clauses.toTypedArray())
        }.get(source).trackData(TAG)

        Logger.d(
            TAG,
            "Group events for (${months.keys.joinToString()}). Source: $source. " +
                    "Amount: ${snapshot.documents.size}"
        )
        return snapshot.documents.map {
            DocHolder(id = it.reference.id, doc = it.data(GroupEventDocument.serializer()))
        }
    }

    private fun List<DocHolder<GroupEventDocument>>.updatedByMonth(): Map<YearMonth, Instant> =
        groupBy { YearMonth.parse(it.doc.yearMonth) }
            .mapNotNull { (month, docs) ->
                docs.mapNotNull { it.doc.updateAt.toInstantOrNull() }.maxOrNull()
                    ?.let { month to it }
            }
            .toMap()

    private suspend fun staleEventMonths(
        groupId: GroupId,
        months: YearMonthRange,
        cacheUpdatedAt: Map<YearMonth, Instant>
    ): Map<YearMonth, Instant?> {
        val serverUpdatedAt = groupSyncFirestore.get(groupId).valueOrNull()
            ?.eventsUpdatedAt.orEmpty()

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
