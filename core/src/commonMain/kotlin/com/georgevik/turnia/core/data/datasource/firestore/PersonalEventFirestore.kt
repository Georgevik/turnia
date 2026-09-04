package com.georgevik.turnia.core.data.datasource.firestore

import com.georgevik.turnia.core.data.datasource.firestore.doc.DocHolder
import com.georgevik.turnia.core.data.datasource.firestore.doc.PersonalEventDocument
import com.georgevik.turnia.core.data.datasource.firestore.errors.GenericFirestoreError
import com.georgevik.turnia.core.data.datasource.firestore.mappers.PersonalEventMapper
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.EventId
import com.georgevik.turnia.core.domain.model.PersonalEvent
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.errorOrNull
import com.georgevik.turnia.core.system.outcomeCatching
import com.georgevik.turnia.core.system.toYearMonth
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Source
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

    suspend fun get(
        userId: UserId,
        from: Instant,
        until: Instant
    ): Outcome<List<DocHolder<PersonalEventDocument>>, GenericFirestoreError> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) {
            val syncUpdates = userSyncFirestore.get(userId)

            val monthRange = YearMonthRange(from.toYearMonth(), until.toYearMonth())

            val cachedEvents = queryEvents(userId, monthRange.toList(), Source.CACHE)
            val monthsWithEvents =
                cachedEvents.distinctBy { it.doc.yearMonth }.map { it.doc.yearMonth }.toSet()
            val monthMissingEvents =
                monthRange.filterNot { monthsWithEvents.contains(it.toString()) }

            if (monthMissingEvents.isNotEmpty()) {
                // Ask for those months that doesn't have events
                queryEvents(userId, monthMissingEvents, Source.SERVER)
            }

            queryEvents(userId, monthRange.toList(), Source.CACHE)
        }

    suspend fun set(uid: UserId, event: PersonalEvent): Outcome<Unit, GenericFirestoreError> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) {
            val doc = personalEventMapper.map(event)
            Logger.d(TAG, "Set personal event document")
            firestore.collection(PATH_EVENTS(uid.value)).document(event.id.value).set(doc)
            markEventsUpdated(uid, event.date.toYearMonth())
        }

    suspend fun delete(
        uid: UserId,
        eventId: EventId,
        eventDate: LocalDate
    ): Outcome<Unit, GenericFirestoreError> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) {
            Logger.d(TAG, "Delete personal event document")
            firestore.collection(PATH_EVENTS(uid.value)).document(eventId.value).delete()
            markEventsUpdated(uid, eventDate.yearMonth)
        }

    private suspend fun markEventsUpdated(uid: UserId, yearMonth: YearMonth) {
        userSyncFirestore.updatePersonalEvents(uid, yearMonth).errorOrNull()?.let { error ->
            Logger.e(TAG, "Error updating personal events sync", error.error)
        }
    }

    private suspend fun queryEvents(
        uid: UserId,
        months: List<YearMonth>,
        source: Source
    ): List<DocHolder<PersonalEventDocument>> {
        val snapshot = firestore.collection(PATH_EVENTS(uid.value)).where {
            PersonalEventDocument.FIELD_YEAR_MONTH inArray months.map { it.toString() }
        }.get(source)

        Logger.d(
            TAG,
            "Personal events for (${months.joinToString()}). Source: ${source}. " +
                    "Amount: ${snapshot.documents.size}. " +
                    "Changes: ${snapshot.documentChanges.size}"
        )
        return snapshot.documents.map { personalEventMapper.map(it) }
    }

    companion object {
        private const val TAG = "PersonalEventFirestore"
        private fun PATH_EVENTS(uid: String) = "users/${uid}/personalEvents"
    }
}
