package com.geoviksoft.turnia.core.data.sharedcalendar.mappers

import com.geoviksoft.turnia.core.data.datasource.firestore.doc.UserPreferencesDocument
import com.geoviksoft.turnia.core.data.datasource.firestorefunctions.requests.SharedCalendarResponse
import com.geoviksoft.turnia.core.data.datasource.firestorefunctions.requests.SharedGroupEventResponse
import com.geoviksoft.turnia.core.data.datasource.firestorefunctions.requests.SharedPersonalEventResponse
import com.geoviksoft.turnia.core.data.datasource.firestorefunctions.requests.SharedPersonalEventTypeResponse
import com.geoviksoft.turnia.core.domain.model.EventId
import com.geoviksoft.turnia.core.domain.model.EventTypeId
import com.geoviksoft.turnia.core.domain.model.GroupEvent
import com.geoviksoft.turnia.core.domain.model.GroupEventType
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.PersonalEvent
import com.geoviksoft.turnia.core.domain.model.PersonalEventType
import com.geoviksoft.turnia.core.domain.model.SharedCalendar
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.system.toInstant
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * What `getSharedCalendar` aggregates, as the calendar the domain draws. There is no way back: a
 * shared calendar is read-only, and nothing here is ever written.
 */
class SharedCalendarMapper {

    fun map(response: SharedCalendarResponse): SharedCalendar {
        val groupTypes = response.groupTypesById()
        val personalTypes = response.personalEventTypes.associate {
            EventTypeId(it.id) to map(it)
        }

        return SharedCalendar(
            groupEvents = response.groupEvents.mapNotNull { map(it, groupTypes) },
            personalEvents = response.personalEvents.mapNotNull { map(it, personalTypes) },
        )
    }

    private fun SharedCalendarResponse.groupTypesById() =
        groupEventTypes.mapValues { (groupId, types) ->
            types.associate { type ->
                EventTypeId(type.id) to GroupEventType(
                    id = EventTypeId(type.id),
                    groupId = GroupId(groupId),
                    groupName = groupNames[groupId].orEmpty(),
                    name = type.name,
                    acronym = type.acronym,
                    description = type.description,
                    startTime = type.startTime,
                    endTime = type.endTime,
                    swappable = type.swappable,
                    defaultColor = type.color.orEmpty(),
                    // The owner's own pick, not the viewer's: it is the owner's calendar, and
                    // recognising a shift on it means seeing it the way they do.
                    userColor = groupEventTypeColors[
                        UserPreferencesDocument.typeColorKey(groupId, type.id)
                    ],
                )
            }
        }

    /** An event whose type the admin removed has nothing left to render. */
    private fun map(
        event: SharedGroupEventResponse,
        types: Map<String, Map<EventTypeId, GroupEventType>>,
    ): GroupEvent? {
        val type = types[event.groupId]?.get(EventTypeId(event.groupEventTypeId)) ?: return null

        return GroupEvent(
            id = EventId(event.eventId),
            groupId = GroupId(event.groupId),
            groupName = type.groupName,
            ownerId = UserId(event.ownerId),
            assigneeId = UserId(event.assigneeId),
            // Every shift here is the owner's own, and the screen is already titled with their
            // name: repeating it on each row would say nothing.
            assigneeName = "",
            type = type,
            date = LocalDate.parse(event.date),
            onSwap = event.onSwap,
            colorHex = type.color,
            // Only a group's own members may read the chain, and this viewer may be in none of the
            // groups: the aggregation does not carry it.
            history = emptyList(),
        )
    }

    private fun map(
        event: SharedPersonalEventResponse,
        types: Map<EventTypeId, PersonalEventType>,
    ): PersonalEvent? {
        val type = event.personalEventTypeId?.let { types[EventTypeId(it)] } ?: return null

        return PersonalEvent(
            id = EventId(event.eventId),
            type = type,
            // Written as an instant, but an event older than that carries a bare date.
            date = runCatching { Instant.parse(event.date) }
                .getOrElse { LocalDate.parse(event.date).toInstant() },
            notes = event.notes.takeIf { it?.isNotEmpty() == true },
        )
    }

    private fun map(type: SharedPersonalEventTypeResponse) = PersonalEventType(
        id = EventTypeId(type.id),
        name = type.name,
        color = type.color,
        acronym = type.acronym,
        description = type.description,
        startTime = type.startTime,
        endTime = type.endTime,
        isDeleted = type.isDeleted,
    )
}
