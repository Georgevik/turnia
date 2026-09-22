package com.geoviksoft.turnia.core.data.datasource.firestore

import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackData
import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackWrite
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.DocHolder
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.PersonalOneOffDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.UserSyncDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.errors.GenericFirestoreError
import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.core.data.user.mappers.PersonalEventMapper
import com.geoviksoft.turnia.core.domain.model.PersonalOneOffEvent
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.outcomeCatching
import com.geoviksoft.turnia.core.system.toInstantOrNull
import com.geoviksoft.turnia.core.system.toTimestamp
import com.geoviksoft.turnia.core.system.toYearMonth
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.YearMonth
import kotlinx.datetime.YearMonthRange
import kotlinx.datetime.yearMonth
import kotlin.time.Instant

/**
 * Interacts with Firestore: `users/{uid}/personalOneOffEvents`
 */
class PersonalOneOffEventFirestore(
    private val firestore: FirebaseFirestore,
    private val personalEventMapper: PersonalEventMapper,
    private val userSyncFirestore: UserSyncFirestore
) {

    private val checkedMarkers = mutableMapOf<Pair<UserId, YearMonth>, Instant>()
    private val checkedMarkersLock = Mutex()

    fun get(
        userId: UserId, from: Instant, until: Instant
    ): Flow<List<DocHolder<PersonalOneOffDocument>>> = flow {
        val months = YearMonthRange(from.toYearMonth(), until.toYearMonth())
        val cachedEvents = queryEvents(userId, months.associateWith { null }, Source.CACHE)

        cachedEvents.areNotDeleted().takeIf { it.isNotEmpty() }?.let { events ->
            emit(events)
        }

        var known = cachedEvents
        var settled = false

        emitAll(
            userSyncFirestore.observe(userId).mapNotNull { sync ->
                val staleMonths = staleEventMonths(
                    months, sync, known.updatedByMonth().withChecked(userId, months)
                )

                if (staleMonths.isEmpty()) {
                    // Nothing moved: the emission before this one still stands
                    if (settled) return@mapNotNull null
                    settled = true
                    return@mapNotNull known.areNotDeleted()
                }

                val serverEvents = queryEvents(
                    userId,
                    staleMonths.mapValues { (_, updatedAt) -> updatedAt?.toTimestamp() },
                    Source.SERVER
                ).associateBy { it.id }.toMutableMap()

                recordChecked(userId, staleMonths.keys, sync)

                val merged = known.map { cached -> serverEvents.remove(cached.id) ?: cached }
                known = merged + serverEvents.values
                settled = true
                known.areNotDeleted()
            })
    }

    suspend fun set(uid: UserId, event: PersonalOneOffEvent): Outcome<Unit, GenericFirestoreError> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) {
            val doc = personalEventMapper.map(event)
            Logger.d(TAG, "Set personal oneOff event document")

            val batch = firestore.batch()
            batch.set(firestore.collection(PATH_EVENTS(uid.value)).document(event.id.value), doc)
            val syncWrite = userSyncFirestore.writePersonalOneOffEvents(batch, uid, event.months())
            batch.commit()
            trackWrite(TAG, "setEvent")
            syncWrite.committed()
        }

    /**
     * [previous] is the event as it was before this edit: moving its dates must also mark the
     * months it leaves as changed, or a device showing only those keeps it on its old date.
     */
    suspend fun update(
        uid: UserId, previous: PersonalOneOffEvent, event: PersonalOneOffEvent
    ): Outcome<Unit, GenericFirestoreError> = outcomeCatching(TAG, { GenericFirestoreError(it) }) {
        Logger.d(TAG, "Update personal oneOff event notes")

        val eventId = event.id
        val batch = firestore.batch()
        batch.updateFields(
            firestore.collection(PATH_EVENTS(uid.value)).document(eventId.value)
        ) {
            PersonalOneOffDocument.FIELD_NAME to event.name
            PersonalOneOffDocument.FIELD_COLOR to event.color
            PersonalOneOffDocument.FIELD_START to event.start.toString()
            PersonalOneOffDocument.FIELD_END to event.end.toString()
            PersonalOneOffDocument.FIELD_ALL_DAY to event.allDay
            PersonalOneOffDocument.FIELD_NOTES to event.notes
            PersonalOneOffDocument.FIELD_YEAR_MONTH_START to event.start.date.yearMonth.toString()
            PersonalOneOffDocument.FIELD_YEAR_MONTH_END to event.end.date.yearMonth.toString()
            PersonalOneOffDocument.FIELD_UPDATE_AT to Timestamp.ServerTimestamp
        }
        val syncWrite = userSyncFirestore.writePersonalOneOffEvents(
            batch, uid, previous.months() + event.months()
        )
        batch.commit()
        trackWrite(TAG, "update")
        syncWrite.committed()
    }

    suspend fun delete(
        uid: UserId, event: PersonalOneOffEvent
    ): Outcome<Unit, GenericFirestoreError> = outcomeCatching(TAG, { GenericFirestoreError(it) }) {
        Logger.d(TAG, "Delete personal oneOff event document")

        val batch = firestore.batch()
        batch.updateFields(
            firestore.collection(PATH_EVENTS(uid.value)).document(event.id.value)
        ) {
            PersonalOneOffDocument.FIELD_IS_DELETED to true
            PersonalOneOffDocument.FIELD_UPDATE_AT to Timestamp.ServerTimestamp
        }
        val syncWrite = userSyncFirestore.writePersonalOneOffEvents(batch, uid, event.months())
        batch.commit()
        trackWrite(TAG, "deleteEvent")
        syncWrite.committed()
    }

    private suspend fun queryEvents(
        uid: UserId, months: Map<YearMonth, Timestamp?>, source: Source
    ): List<DocHolder<PersonalOneOffDocument>> {
        if (months.isEmpty()) return emptyList()

        val snapshotResults = coroutineScope {
            months.map { (month, sinceUpdateAt) ->
                async {
                    val monthStr = month.toString()
                    firestore.collection(PATH_EVENTS(uid.value)).where {
                        val inMonth =
                            (PersonalOneOffDocument.FIELD_YEAR_MONTH_START lessThanOrEqualTo monthStr) and
                                    (PersonalOneOffDocument.FIELD_YEAR_MONTH_END greaterThanOrEqualTo monthStr)

                        sinceUpdateAt?.let { inMonth and (PersonalOneOffDocument.FIELD_UPDATE_AT greaterThan it) }
                            ?: inMonth
                    }.get(source).trackData(TAG, "events($source)")
                }
            }.awaitAll()
        }

        return snapshotResults.flatMap { snapshotResult ->
            Logger.d(
                TAG,
                "Personal oneOff events for (${months.keys.joinToString()}). Source: ${source}. " + "Amount: ${snapshotResult.documents.size}. " + "Changes: ${snapshotResult.documentChanges.size}"
            )
            snapshotResult.documents.map { doc -> personalEventMapper.mapOneOff(doc) }
        }
    }

    private fun PersonalOneOffEvent.months(): Set<YearMonth> =
        (start.date.yearMonth..end.date.yearMonth).toSet()

    private fun List<DocHolder<PersonalOneOffDocument>>.areNotDeleted() =
        filterNot { it.doc.isDeleted }

    private fun List<DocHolder<PersonalOneOffDocument>>.updatedByMonth(): Map<YearMonth, Instant> {
        val events = this

        return buildMap {
            events.forEach { event ->
                val ymStart = YearMonth.parse(event.doc.yearMonthStart)
                val ymEnd = YearMonth.parse(event.doc.yearMonthEnd)

                (ymStart..ymEnd).forEach { month ->
                    val updateAt = event.doc.updateAt.toInstantOrNull() ?: return@forEach
                    val existing = get(month)

                    put(month, if (existing != null) maxOf(updateAt, existing) else updateAt)
                }
            }
        }
    }


    /** What the cache holds of each month, or the marker it was last checked at if that is newer. */
    private suspend fun Map<YearMonth, Instant>.withChecked(
        uid: UserId, months: YearMonthRange,
    ): Map<YearMonth, Instant> = checkedMarkersLock.withLock {
        months.mapNotNull { month ->
            val cached = this[month]
            val checked = checkedMarkers[uid to month]
            val newest =
                if (cached != null && checked != null) maxOf(cached, checked) else cached ?: checked
            newest?.let { month to it }
        }.toMap()
    }

    private suspend fun recordChecked(uid: UserId, months: Set<YearMonth>, sync: UserSyncDocument) =
        checkedMarkersLock.withLock {
            months.forEach { month ->
                sync.personalOneOffEventsUpdatedAt[month]?.updatedAt.toInstantOrNull()
                    ?.let { checkedMarkers[uid to month] = it }
            }
        }

    private fun staleEventMonths(
        months: YearMonthRange, sync: UserSyncDocument, cacheUpdatedAt: Map<YearMonth, Instant>
    ): Map<YearMonth, Instant?> {
        val serverUpdatedAt = sync.personalOneOffEventsUpdatedAt

        // Each stale month keeps its own cursor: what this device already holds of *that* month.
        val stale = months.mapNotNull { month ->
            val server =
                serverUpdatedAt[month]?.updatedAt.toInstantOrNull() ?: return@mapNotNull null
            val cached = cacheUpdatedAt[month]

            if (cached == null || server > cached) month to cached else null
        }.toMap()

        Logger.d(TAG, "Stale oneoff months: ${stale.keys.joinToString().ifEmpty { "none" }}")
        return stale
    }

    companion object {
        private const val TAG = "PersonalOneOffEventFirestore"
        private fun PATH_EVENTS(uid: String) = "users/${uid}/personalOneOffEvents"
    }
}
