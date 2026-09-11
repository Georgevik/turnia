package com.geoviksoft.turnia.core.domain.repository

import com.geoviksoft.turnia.core.domain.model.EventId
import com.geoviksoft.turnia.core.domain.model.EventTypeId
import com.geoviksoft.turnia.core.domain.model.PersonalEvent
import com.geoviksoft.turnia.core.domain.model.PersonalEventType
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.system.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * The current user's personal events and the types they are created from. Unlike group
 * types these belong to no group and carry their own [PersonalEventType.color], so the
 * user can create and edit them freely.
 */
interface PersonalEventRepository {
    fun getMyEventTypes(includeDeleted: Boolean = false): Flow<List<PersonalEventType>>

    suspend fun addEvent(event: PersonalEvent)

    suspend fun deleteEvent(eventId: EventId, eventDate: LocalDate)

    /**
     * Saves a personal event's note. A blank note deletes it.
     */
    suspend fun saveNotes(
        eventId: EventId,
        eventDate: LocalDate,
        notes: String?
    ): Outcome<Unit, Unit>

    suspend fun saveEventType(type: PersonalEventType): Outcome<Unit, Unit>

    suspend fun deleteEventType(typeId: EventTypeId): Outcome<Unit, Unit>

    /** Cached events first, still loading, then the server's if it had anything newer. */
    fun getEvents(
        uid: UserId,
        date: LocalDate,
        monthDelta: Int = 1,
    ): Flow<List<PersonalEvent>>
}
