package com.georgevik.turnia.core.domain.repository

import com.georgevik.turnia.core.domain.model.PersonalEvent
import com.georgevik.turnia.core.domain.model.PersonalEventType
import com.georgevik.turnia.core.system.Outcome
import kotlinx.datetime.LocalDate

/**
 * The current user's personal events and the types they are created from. Unlike group
 * types these belong to no group and carry their own [PersonalEventType.color], so the
 * user can create and edit them freely.
 */
interface PersonalEventRepository {

    suspend fun getEventTypes(): List<PersonalEventType>

    suspend fun addEvent(event: PersonalEvent)

    suspend fun deleteEvent(eventId: String)

    suspend fun saveEventType(type: PersonalEventType): Outcome<Unit, Unit>

    suspend fun getEvents(
        userId: String,
        date: LocalDate,
        monthDelta: Int = 1
    ): Result<List<PersonalEvent>>

    suspend fun getEventType(typeId: String): Result<PersonalEventType>
}
