package com.georgevik.turnia.core.domain.repository

import com.georgevik.turnia.core.domain.model.PersonalEvent
import com.georgevik.turnia.core.domain.model.PersonalEventType
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate

/**
 * The current user's personal event templates. Unlike group types these belong to
 * no group and carry their own [PersonalEventType.color]; the user can create and
 * edit them freely.
 */
interface PersonalEventRepository {

    suspend fun getPersonalEventTypes(): List<PersonalEventType>

    suspend fun addPersonalEvent(event: PersonalEvent)

    suspend fun deletePersonalEvent(eventId: String)

    suspend fun update(typeId: String?, type: PersonalEventType): Result<Unit>

    suspend fun retrievePersonalEvents(
        userId: String,
        date: LocalDate,
        monthDelta: Int = 1
    ): Result<List<PersonalEvent>>


    suspend fun getEventType(typeId: String): Result<PersonalEventType>
}
