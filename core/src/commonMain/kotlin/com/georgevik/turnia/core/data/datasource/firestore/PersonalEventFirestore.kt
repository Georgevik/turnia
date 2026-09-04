package com.georgevik.turnia.core.data.datasource.firestore

import com.georgevik.turnia.core.data.datasource.firestore.analytics.trackData
import com.georgevik.turnia.core.data.datasource.firestore.analytics.trackWrite
import com.georgevik.turnia.core.data.datasource.firestore.doc.DocHolder
import com.georgevik.turnia.core.data.datasource.firestore.doc.PersonalEventDocument
import com.georgevik.turnia.core.data.datasource.firestore.doc.UserSyncDocument
import com.georgevik.turnia.core.data.datasource.firestore.errors.GenericFirestoreError
import com.georgevik.turnia.core.data.datasource.firestore.mappers.PersonalEventMapper
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.EventId
import com.georgevik.turnia.core.domain.model.PersonalEvent
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.outcomeCatching
import com.georgevik.turnia.core.system.toFailure
import com.georgevik.turnia.core.system.toInstantOrNull
import com.georgevik.turnia.core.system.toSuccess
import com.georgevik.turnia.core.system.toTimestamp
import com.georgevik.turnia.core.system.toYearMonth
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
    ): Flow<Outcome<List<DocHolder<PersonalEventDocument>>, GenericFirestoreError>> =
        flow<Outcome<List<DocHolder<PersonalEventDocument>>, GenericFirestoreError>> {
            val months = YearMonthRange(from.toYearMonth(), until.toYearMonth())
            val cachedEvents = queryEvents(userId, months.associateWith { null }, Source.CACHE)
            emit(cachedEvents.filterNot { it.doc.isDeleted }.toSuccess())

            var known = cachedEvents
            // An empty cache is fetched whole once, and only once: a range with no events of its
            // own has no markers either, and would otherwise ask again on every emission.
            var fetched = cachedEvents.isNotEmpty()

            emitAll(
                userSyncFirestore.observe(userId).mapNotNull { sync ->
                    val staleMonths =
                        if (!fetched) months.associateWith { null }
                        else staleEventMonths(months, sync, known.updatedByMonth())
                    // Nothing moved: the emission before this one still stands.
                    if (staleMonths.isEmpty()) return@mapNotNull null

                    val serverEvents = queryEvents(
                        userId,
                        staleMonths.mapValues { (_, updatedAt) -> updatedAt?.toTimestamp() },
                        Source.SERVER
                    ).associateBy { it.id }.toMutableMap()

                    val merged = known.map { cached -> serverEvents.remove(cached.id) ?: cached }
                    known = merged + serverEvents.values
                    fetched = true
                    known.filterNot { it.doc.isDeleted }.toSuccess()
                }
            )
        }.catch { throwable ->
            Logger.e(TAG, "Failed to read personal events", throwable)
            emit(GenericFirestoreError(throwable).toFailure())
        }

    suspend fun set(uid: UserId, event: PersonalEvent): Outcome<Unit, GenericFirestoreError> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) {
            val doc = personalEventMapper.map(event)
            Logger.d(TAG, "Set personal event document")

            val batch = firestore.batch()
            batch.set(firestore.collection(PATH_EVENTS(uid.value)).document(event.id.value), doc)
            userSyncFirestore.writePersonalEvents(batch, uid, event.date.toYearMonth())
            batch.commit()
            trackWrite(TAG)
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
            userSyncFirestore.writePersonalEvents(batch, uid, eventDate.yearMonth)
            batch.commit()
            trackWrite(TAG)
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
        }.get(source).trackData(TAG)

        Logger.d(
            TAG,
            "Personal events for (${months.keys.joinToString()}). Source: ${source}. " +
                    "Amount: ${snapshot.documents.size}. " +
                    "Changes: ${snapshot.documentChanges.size}"
        )
        return snapshot.documents.map { personalEventMapper.map(it) }
    }

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
