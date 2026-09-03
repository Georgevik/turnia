package com.georgevik.turnia.core.data.datasource.firestore

import com.georgevik.turnia.core.data.datasource.firestore.doc.DocHolder
import com.georgevik.turnia.core.data.datasource.firestore.doc.PersonalEventDocument
import com.georgevik.turnia.core.data.datasource.firestore.errors.GenericFirestoreError
import com.georgevik.turnia.core.data.datasource.firestore.mappers.PersonalEventMapper
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.PersonalEvent
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.outcomeCatching
import com.georgevik.turnia.core.system.toYearMonth
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Source
import kotlinx.datetime.YearMonth
import kotlinx.datetime.YearMonthRange
import kotlin.time.Instant

/**
 * Interacts with Firestore: `users/{uid}/personalEvents`
 */
class PersonalEventFirestore(
    private val firestore: FirebaseFirestore,
    private val personalEventMapper: PersonalEventMapper
) {
    private val queriedYearMonth = mutableSetOf<Pair<String, YearMonth>>()

    suspend fun get(
        uid: String,
        from: Instant,
        until: Instant
    ): Outcome<List<DocHolder<PersonalEventDocument>>, GenericFirestoreError> =
        outcomeCatching({ GenericFirestoreError(it) }) {
            val monthRange = YearMonthRange(from.toYearMonth(), until.toYearMonth())
            val serverMonths = monthRange.filterNot { queriedYearMonth.contains(uid to it) }

            downloadPersonalEvents(uid, serverMonths)
            serverMonths.forEach { queriedYearMonth.add(uid to it) }
            getPersonalEventsFromCache(uid, monthRange)
        }

    private suspend fun getPersonalEventsFromCache(
        uid: String,
        range: YearMonthRange
    ): List<DocHolder<PersonalEventDocument>> {
        val cacheSnapshot = firestore.collection(PATH_EVENTS(uid)).where {
            PersonalEventDocument.FIELD_YEAR_MONTH inArray range.map { it.toString() }
        }.get(Source.CACHE)
        Logger.d(
            TAG,
            "Personal events. Cached: ${cacheSnapshot.metadata.isFromCache}. " +
                    "Amount: ${cacheSnapshot.documents.size}. " +
                    "Changes: ${cacheSnapshot.documentChanges.size}"
        )
        return cacheSnapshot.documents.map { personalEventMapper.map(it) }
    }

    private suspend fun downloadPersonalEvents(uid: String, months: List<YearMonth>) {
        if (months.isEmpty()) return

        val snapshot = firestore.collection(PATH_EVENTS(uid)).where {
            PersonalEventDocument.FIELD_YEAR_MONTH inArray months.map { it.toString() }
        }.get()
        Logger.d(
            TAG,
            "Downloaded Personal events. Cache: ${snapshot.metadata.isFromCache}. " +
                    "Amount: ${snapshot.documents.size}. " +
                    "Changes: ${snapshot.documentChanges.size}"
        )
    }

    suspend fun set(uid: String, event: PersonalEvent): Outcome<Unit, GenericFirestoreError> =
        outcomeCatching({ GenericFirestoreError(it) }) {
            val doc = personalEventMapper.map(event)
            Logger.d(TAG, "Set personal event document")
            firestore.collection(PATH_EVENTS(uid)).document(event.id).set(doc)
        }

    suspend fun delete(uid: String, eventId: String): Outcome<Unit, GenericFirestoreError> =
        outcomeCatching({ GenericFirestoreError(it) }) {
            Logger.d(TAG, "Delete personal event document")
            firestore.collection(PATH_EVENTS(uid)).document(eventId).delete()
        }

    companion object {
        private const val TAG = "PersonalEventTypesFirestore"
        private fun PATH_EVENTS(uid: String) = "users/${uid}/personalEvents"
    }
}
