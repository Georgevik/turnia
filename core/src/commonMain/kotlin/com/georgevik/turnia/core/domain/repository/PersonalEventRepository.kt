package com.georgevik.turnia.core.domain.repository

import com.georgevik.turnia.core.domain.model.EventId
import com.georgevik.turnia.core.domain.model.EventTypeId
import com.georgevik.turnia.core.domain.model.PersonalEvent
import com.georgevik.turnia.core.domain.model.PersonalEventType
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.system.Outcome
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

    suspend fun saveEventType(type: PersonalEventType): Outcome<Unit, Unit>

    suspend fun deleteEventType(typeId: EventTypeId): Outcome<Unit, Unit>

    fun getEvents(
        uid: UserId,
        date: LocalDate,
        monthDelta: Int = 1
    ): Flow<Outcome<List<PersonalEvent>, Unit>>
}
