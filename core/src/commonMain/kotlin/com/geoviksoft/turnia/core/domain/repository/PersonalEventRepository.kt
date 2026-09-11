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
     * Guarda la nota de un evento personal. En blanco la borra.
     *
     * Sólo eventos personales: el documento de un evento de grupo lo leen todos los miembros, así
     * que no puede llevar nada privado (ver *No private fields on shared docs* en CLAUDE.md).
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
