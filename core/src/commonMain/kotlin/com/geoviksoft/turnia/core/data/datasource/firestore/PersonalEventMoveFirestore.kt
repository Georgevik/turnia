package com.geoviksoft.turnia.core.data.datasource.firestore

import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.PendingWrite
import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackData
import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackWrite
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.DocHolder
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.GroupEventDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.PersonalEventDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.PersonalEventTypeDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.errors.GenericFirestoreError
import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.core.data.user.PersonalEventMoveStore
import com.geoviksoft.turnia.core.data.user.mappers.PersonalEventMapper
import com.geoviksoft.turnia.core.domain.model.EventTypeId
import com.geoviksoft.turnia.core.domain.model.GroupEventType
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.PersonalTypedEvent
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.mapError
import com.geoviksoft.turnia.core.system.outcomeCatching
import com.geoviksoft.turnia.core.system.parseEventDate
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.yearMonth

/**
 * Moves personal events into a group: `users/{uid}/personalEvents` → `groups/{groupId}/events`.
 *
 * The one writer that spans both, so a day is never shown twice: each commit creates the shifts,
 * soft-deletes the personal events and moves both calendars' markers together. Everything it reads
 * comes from the server — a stale answer here would double-book a day, which is what the app exists
 * to prevent.
 */
class PersonalEventMoveFirestore(
    private val firestore: FirebaseFirestore,
    private val personalEventMapper: PersonalEventMapper,
    private val userSyncFirestore: UserSyncFirestore,
    private val groupSyncFirestore: GroupSyncFirestore,
    private val groupEventExtrasFirestore: GroupEventExtrasFirestore,
) : PersonalEventMoveStore {

    /** The still-personal events of [typeId] dated [from] onward. */
    suspend fun candidates(
        uid: UserId,
        typeId: EventTypeId,
        from: LocalDate,
    ): Outcome<List<DocHolder<PersonalEventDocument>>, GenericFirestoreError> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) {
            val snapshot = firestore.collection(PATH_PERSONAL_EVENTS(uid.value)).where {
                (FIELD_TYPE_ID equalTo typeId.value) and
                        (PersonalEventDocument.FIELD_YEAR_MONTH greaterThanOrEqualTo from.yearMonth.toString())
            }.get(Source.SERVER).trackData(TAG, "candidates(SERVER)")

            snapshot.documents.map { personalEventMapper.map(it) }.filter { holder ->
                // The month is only a coarse bound: the window starts on a day, not on the 1st.
                val date = parseEventDate(holder.doc.date)
                !holder.doc.isDeleted && date != null && date >= from
            }
        }

    override suspend fun heldDates(
        uid: UserId,
        groupId: GroupId,
        months: Set<YearMonth>,
    ): Outcome<Set<LocalDate>, Unit> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) {
            // Firestore caps an `in` filter at 30 values.
            months.map { it.toString() }.chunked(MAX_IN_VALUES).flatMap { slice ->
                firestore.collection(PATH_GROUP_EVENTS(groupId.value)).where {
                    (GroupEventDocument.FIELD_ASSIGNEE_ID equalTo uid.value) and
                            (GroupEventDocument.FIELD_YEAR_MONTH inArray slice)
                }.get(Source.SERVER).trackData(TAG, "heldDates(SERVER)").documents
            }
                .map { it.data(GroupEventDocument.serializer()) }
                .filterNot { it.isDeleted }
                .mapNotNull { parseEventDate(it.date) }
                .toSet()
        }.mapError { error -> Logger.e(TAG, "Failed to read the days already held", error.error) }

    override suspend fun commit(
        uid: UserId,
        target: GroupEventType,
        events: List<PersonalTypedEvent>,
        deleteType: EventTypeId?,
    ): Outcome<Unit, Unit> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) {
            Logger.d(TAG, "Move ${events.size} personal events to a group, deleting the type: ${deleteType != null}")

            val batch = firestore.batch()
            var documents = 0
            val pending = mutableListOf<PendingWrite>()

            events.forEach { event ->
                // The personal event's id: a move run again after a failure cannot duplicate a shift.
                batch.set(
                    firestore.collection(PATH_GROUP_EVENTS(target.groupId.value)).document(event.id.value),
                    GroupEventDocument(
                        ownerId = uid.value,
                        assigneeId = uid.value,
                        groupEventTypeId = target.id.value,
                        date = event.date.toString(),
                        yearMonth = event.date.yearMonth.toString(),
                    ),
                )
                batch.updateFields(
                    firestore.collection(PATH_PERSONAL_EVENTS(uid.value)).document(event.id.value)
                ) {
                    PersonalEventDocument.FIELD_IS_DELETED to true
                    PersonalEventDocument.FIELD_UPDATE_AT to Timestamp.ServerTimestamp
                }
                documents += 2

                event.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                    groupEventExtrasFirestore.set(
                        batch,
                        uid,
                        event.id,
                        groupEventExtrasFirestore.document(target.groupId, event.date.yearMonth, notes),
                    )
                    documents++
                }
            }

            if (events.isNotEmpty()) {
                val months = events.map { it.date.yearMonth }.toSet()
                pending += userSyncFirestore.writePersonalEvents(batch, uid, months)
                pending += groupSyncFirestore.writeEvents(batch, target.groupId, months)
                val noteMonths = events.filter { !it.notes.isNullOrBlank() }.map { it.date.yearMonth }.toSet()
                if (noteMonths.isNotEmpty()) {
                    pending += userSyncFirestore.writeGroupEventExtras(batch, uid, noteMonths)
                }
            }

            deleteType?.let { typeId ->
                batch.updateFields(
                    firestore.collection(PATH_PERSONAL_TYPES(uid.value)).document(typeId.value)
                ) {
                    PersonalEventTypeDocument.FIELD_IS_DELETED to true
                    PersonalEventTypeDocument.FIELD_UPDATE_AT to Timestamp.ServerTimestamp
                }
                documents++
                pending += userSyncFirestore.writePersonalEventTypes(batch, uid)
            }

            batch.commit()
            trackWrite(TAG, "move", documents = documents)
            pending.forEach { it.committed() }
        }.mapError { error -> Logger.e(TAG, "Failed to move personal events to a group", error.error) }

    private companion object {
        const val TAG = "PersonalEventMoveFirestore"
        const val FIELD_TYPE_ID = "typeId"
        const val MAX_IN_VALUES = 30
        fun PATH_PERSONAL_EVENTS(uid: String) = "users/${uid}/personalEvents"
        fun PATH_PERSONAL_TYPES(uid: String) = "users/${uid}/personalEventTypes"
        fun PATH_GROUP_EVENTS(groupId: String) = "groups/${groupId}/events"
    }
}
