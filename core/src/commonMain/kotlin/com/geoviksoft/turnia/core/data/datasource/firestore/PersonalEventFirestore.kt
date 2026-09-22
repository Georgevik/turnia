package com.geoviksoft.turnia.core.data.datasource.firestore

import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackData
import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackWrite
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.DocHolder
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.PersonalEventDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.UserSyncDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.errors.GenericFirestoreError
import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.core.data.user.mappers.PersonalEventMapper
import com.geoviksoft.turnia.core.domain.model.EventId
import com.geoviksoft.turnia.core.domain.model.PersonalTypedEvent
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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.YearMonthRange
import kotlinx.datetime.yearMonth
import kotlin.time.Instant

/**
 * Interacts with Firestore: `users/{uid}/personalEvents`
 */
class PersonalEventFirestore(
    private val firestore: FirebaseFirestore,
    private val personalEventMapper: PersonalEventMapper,
    private val userSyncFirestore: UserSyncFirestore
) {

    fun get(
        userId: UserId,
        from: Instant,
        until: Instant
    ): Flow<List<DocHolder<PersonalEventDocument>>> =
        flow {
            val months = YearMonthRange(from.toYearMonth(), until.toYearMonth())
            val cachedEvents = queryEvents(userId, months.associateWith { null }, Source.CACHE)

            cachedEvents.areNotDeleted().takeIf { it.isNotEmpty() }?.let { events ->
                emit(events)
            }

            var known = cachedEvents
            var settled = false

            emitAll(
                userSyncFirestore.observe(userId).mapNotNull { sync ->
                    val staleMonths = staleEventMonths(months, sync, known.updatedByMonth())

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

                    val merged = known.map { cached -> serverEvents.remove(cached.id) ?: cached }
                    known = merged + serverEvents.values
                    settled = true
                    known.areNotDeleted()
                }
            )
        }

    suspend fun set(uid: UserId, event: PersonalTypedEvent): Outcome<Unit, GenericFirestoreError> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) {
            val doc = personalEventMapper.map(event)
            Logger.d(TAG, "Set personal event document")

            val batch = firestore.batch()
            batch.set(firestore.collection(PATH_EVENTS(uid.value)).document(event.id.value), doc)
            val syncWrite =
                userSyncFirestore.writePersonalEvents(batch, uid, event.date.yearMonth)
            batch.commit()
            trackWrite(TAG, "setEvent")
            syncWrite.committed()
        }

    suspend fun updateNotes(
        uid: UserId,
        eventId: EventId,
        eventDate: LocalDate,
        notes: String?,
    ): Outcome<Unit, GenericFirestoreError> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) {
            Logger.d(TAG, "Update personal event notes")

            val batch = firestore.batch()
            batch.updateFields(
                firestore.collection(PATH_EVENTS(uid.value)).document(eventId.value)
            ) {
                PersonalEventDocument.FIELD_NOTES to notes
                PersonalEventDocument.FIELD_UPDATE_AT to Timestamp.ServerTimestamp
            }
            val syncWrite = userSyncFirestore.writePersonalEvents(batch, uid, eventDate.yearMonth)
            batch.commit()
            trackWrite(TAG, "updateNotes")
            syncWrite.committed()
        }

    suspend fun delete(
        uid: UserId,
        eventId: EventId,
        eventDate: LocalDate
    ): Outcome<Unit, GenericFirestoreError> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) {
            Logger.d(TAG, "Delete personal event document")

            val batch = firestore.batch()
            batch.updateFields(
                firestore.collection(PATH_EVENTS(uid.value)).document(eventId.value)
            ) {
                PersonalEventDocument.FIELD_IS_DELETED to true
                PersonalEventDocument.FIELD_UPDATE_AT to Timestamp.ServerTimestamp
            }
            val syncWrite = userSyncFirestore.writePersonalEvents(batch, uid, eventDate.yearMonth)
            batch.commit()
            trackWrite(TAG, "deleteEvent")
            syncWrite.committed()
        }

    private suspend fun queryEvents(
        uid: UserId,
        months: Map<YearMonth, Timestamp?>,
        source: Source
    ): List<DocHolder<PersonalEventDocument>> {
        if (months.isEmpty()) return emptyList()

        val snapshot = firestore.collection(PATH_EVENTS(uid.value)).where {
            val clauses = months.map { (month, sinceUpdateAt) ->
                val inMonth = PersonalEventDocument.FIELD_YEAR_MONTH equalTo month.toString()
                val changed =
                    sinceUpdateAt?.let { PersonalEventDocument.FIELD_UPDATE_AT greaterThan it }

                if (changed == null) inMonth else inMonth and changed
            }

            any(*clauses.toTypedArray())
        }.get(source).trackData(TAG, "events($source)")

        Logger.d(
            TAG,
            "Personal events for (${months.keys.joinToString()}). Source: ${source}. " +
                    "Amount: ${snapshot.documents.size}. " +
                    "Changes: ${snapshot.documentChanges.size}"
        )
        return snapshot.documents.map { personalEventMapper.map(it) }
    }

    private fun List<DocHolder<PersonalEventDocument>>.areNotDeleted() =
        filterNot { it.doc.isDeleted }

    private fun List<DocHolder<PersonalEventDocument>>.updatedByMonth(): Map<YearMonth, Instant> =
        this
            .groupBy { YearMonth.parse(it.doc.yearMonth) }
            .mapNotNull { (month, docs) ->
                docs.mapNotNull { it.doc.updateAt.toInstantOrNull() }.maxOrNull()
                    ?.let { month to it }
            }
            .toMap()

    private fun staleEventMonths(
        months: YearMonthRange,
        sync: UserSyncDocument,
        cacheUpdatedAt: Map<YearMonth, Instant>
    ): Map<YearMonth, Instant?> {
        val serverUpdatedAt = sync.personalEventsUpdatedAt

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

    companion object {
        private const val TAG = "PersonalEventFirestore"
        private fun PATH_EVENTS(uid: String) = "users/${uid}/personalEvents"
    }
}
