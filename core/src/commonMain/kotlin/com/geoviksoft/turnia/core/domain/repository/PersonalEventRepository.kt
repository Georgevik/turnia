package com.geoviksoft.turnia.core.domain.repository

import com.geoviksoft.turnia.core.domain.model.EventId
import com.geoviksoft.turnia.core.domain.model.EventTypeId
import com.geoviksoft.turnia.core.domain.model.GroupEventType
import com.geoviksoft.turnia.core.domain.model.MoveError
import com.geoviksoft.turnia.core.domain.model.MoveResult
import com.geoviksoft.turnia.core.domain.model.MoveScope
import com.geoviksoft.turnia.core.domain.model.PersonalEventType
import com.geoviksoft.turnia.core.domain.model.PersonalOneOffEvent
import com.geoviksoft.turnia.core.domain.model.PersonalTypedEvent
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

    suspend fun addEvent(event: PersonalTypedEvent)

    suspend fun deleteEvent(eventId: EventId, eventDate: LocalDate)

    /**
     * Saves a personal event's note. A blank note deletes it.
     */
    suspend fun saveNotes(
        eventId: EventId,
        eventDate: LocalDate,
        notes: String?
    ): Outcome<Unit, Unit>

    /** [isNew] tells a created type from an edited one, which only the caller knows. */
    suspend fun saveEventType(type: PersonalEventType, isNew: Boolean): Outcome<Unit, Unit>

    /** Creates every type in one write, or none of them. */
    suspend fun createEventTypes(types: List<PersonalEventType>): Outcome<Unit, Unit>

    suspend fun deleteEventType(typeId: EventTypeId): Outcome<Unit, Unit>

    /** Cached events first, still loading, then the server's if it had anything newer. */
    fun getEvents(
        uid: UserId,
        date: LocalDate,
        monthDelta: Int = 1,
    ): Flow<List<PersonalTypedEvent>>

    /** Like [getEvents]: every one-off event touching a month of the window, even partly. */
    fun getOneOffEvents(
        uid: UserId,
        date: LocalDate,
        monthDelta: Int = 1,
    ): Flow<List<PersonalOneOffEvent>>

    suspend fun addOneOffEvent(event: PersonalOneOffEvent): Outcome<Unit, Unit>

    /** [previous] is the event before the edit: the months it leaves have to learn it left. */
    suspend fun updateOneOffEvent(
        previous: PersonalOneOffEvent,
        event: PersonalOneOffEvent,
    ): Outcome<Unit, Unit>

    suspend fun deleteOneOffEvent(event: PersonalOneOffEvent): Outcome<Unit, Unit>

    /**
     * The events of [event]'s type a "move all" would carry: every one still personal and dated from
     * the start of the retention window onward, [event] included. Read from the server, since a
     * cached answer could miss events made on another device.
     */
    suspend fun moveCandidates(event: PersonalTypedEvent): Outcome<List<PersonalTypedEvent>, MoveError>

    /**
     * Turns [events] into shifts of [target] the user holds, skipping every day where they already
     * hold one in that group. With [MoveScope.All] the personal type is deleted once they are all
     * moved or skipped. A move that fails part-way keeps what it moved and the type, so running it
     * again finishes it.
     */
    suspend fun moveToGroup(
        events: List<PersonalTypedEvent>,
        target: GroupEventType,
        scope: MoveScope,
    ): Outcome<MoveResult, MoveError>
}
