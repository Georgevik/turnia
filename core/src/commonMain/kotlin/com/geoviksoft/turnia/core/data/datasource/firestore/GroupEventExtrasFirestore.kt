package com.geoviksoft.turnia.core.data.datasource.firestore

import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackData
import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackWrite
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.DocHolder
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.GroupEventExtrasDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.UserSyncDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.errors.GenericFirestoreError
import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.core.domain.model.EventId
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.outcomeCatching
import com.geoviksoft.turnia.core.system.toInstantOrNull
import com.geoviksoft.turnia.core.system.toTimestamp
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.WriteBatch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.datetime.YearMonth
import kotlin.time.Instant

/**
 * Interacts with Firestore: `users/{uid}/groupEventExtras`
 *
 * The user's own notes on group events, keyed by the event's id. Read like `personalEvents`: from the
 * cache, and from the server only for the months whose marker moved past it. A month without notes
 * has no documents and no marker, so it costs nothing.
 */
class GroupEventExtrasFirestore(
    private val firestore: FirebaseFirestore,
    private val userSyncFirestore: UserSyncFirestore,
) {

    /** The notes of [months], cleared ones left out. */
    fun get(
        userId: UserId,
        months: Collection<YearMonth>,
    ): Flow<List<DocHolder<GroupEventExtrasDocument>>> =
        flow {
            val cached = query(userId, months.associateWith { null }, Source.CACHE)
            cached.withNotes().takeIf { it.isNotEmpty() }?.let { emit(it) }

            var known = cached
            var settled = false

            emitAll(
                userSyncFirestore.observe(userId).mapNotNull { sync ->
                    val staleMonths = staleMonths(months, sync, known.updatedByMonth())

                    if (staleMonths.isEmpty()) {
                        if (settled) return@mapNotNull null
                        settled = true
                        return@mapNotNull known.withNotes()
                    }

                    val server = query(
                        userId,
                        staleMonths.mapValues { (_, updatedAt) -> updatedAt?.toTimestamp() },
                        Source.SERVER,
                    ).associateBy { it.id }.toMutableMap()

                    val merged = known.map { cachedDoc -> server.remove(cachedDoc.id) ?: cachedDoc }
                    known = merged + server.values
                    settled = true
                    known.withNotes()
                }
            )
        }

    suspend fun set(
        uid: UserId,
        groupId: GroupId,
        eventId: EventId,
        yearMonth: YearMonth,
        notes: String?,
    ): Outcome<Unit, GenericFirestoreError> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) {
            Logger.d(TAG, "Set group event notes")

            val batch = firestore.batch()
            set(batch, uid, eventId, document(groupId, yearMonth, notes))
            val syncWrite = userSyncFirestore.writeGroupEventExtras(batch, uid, setOf(yearMonth))
            batch.commit()
            trackWrite(TAG, "setNote")
            syncWrite.committed()
        }

    /** Adds the note to a caller's [batch]; the caller moves the month's marker in the same commit. */
    fun set(batch: WriteBatch, uid: UserId, eventId: EventId, document: GroupEventExtrasDocument) {
        batch.set(firestore.collection(PATH_EXTRAS(uid.value)).document(eventId.value), document)
    }

    fun document(groupId: GroupId, yearMonth: YearMonth, notes: String?) =
        GroupEventExtrasDocument(
            groupId = groupId.value,
            yearMonth = yearMonth.toString(),
            notes = notes,
        )

    private suspend fun query(
        uid: UserId,
        months: Map<YearMonth, Timestamp?>,
        source: Source,
    ): List<DocHolder<GroupEventExtrasDocument>> {
        if (months.isEmpty()) return emptyList()

        val snapshot = firestore.collection(PATH_EXTRAS(uid.value)).where {
            val clauses = months.map { (month, sinceUpdateAt) ->
                val inMonth = GroupEventExtrasDocument.FIELD_YEAR_MONTH equalTo month.toString()
                val changed =
                    sinceUpdateAt?.let { GroupEventExtrasDocument.FIELD_UPDATE_AT greaterThan it }

                if (changed == null) inMonth else inMonth and changed
            }

            any(*clauses.toTypedArray())
        }.get(source).trackData(TAG, "extras($source)")

        Logger.d(
            TAG,
            "Group event notes for (${months.keys.joinToString()}). Source: $source. " +
                    "Amount: ${snapshot.documents.size}"
        )
        return snapshot.documents.map {
            DocHolder(it.id, it.data(GroupEventExtrasDocument.serializer()))
        }
    }

    private fun List<DocHolder<GroupEventExtrasDocument>>.withNotes() =
        filterNot { it.doc.notes.isNullOrBlank() }

    private fun List<DocHolder<GroupEventExtrasDocument>>.updatedByMonth(): Map<YearMonth, Instant> =
        groupBy { YearMonth.parse(it.doc.yearMonth) }
            .mapNotNull { (month, docs) ->
                docs.mapNotNull { it.doc.updateAt.toInstantOrNull() }.maxOrNull()
                    ?.let { month to it }
            }
            .toMap()

    private fun staleMonths(
        months: Collection<YearMonth>,
        sync: UserSyncDocument,
        cacheUpdatedAt: Map<YearMonth, Instant>,
    ): Map<YearMonth, Instant?> =
        months.mapNotNull { month ->
            val server = sync.groupEventExtrasUpdatedAt[month]?.updatedAt.toInstantOrNull()
                ?: return@mapNotNull null
            val cached = cacheUpdatedAt[month]

            if (cached == null || server > cached) month to cached else null
        }.toMap()

    private companion object {
        const val TAG = "GroupEventExtrasFirestore"
        fun PATH_EXTRAS(uid: String) = "users/${uid}/groupEventExtras"
    }
}
