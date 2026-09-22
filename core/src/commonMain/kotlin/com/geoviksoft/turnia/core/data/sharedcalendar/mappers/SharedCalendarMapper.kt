package com.geoviksoft.turnia.core.data.sharedcalendar.mappers

import com.geoviksoft.turnia.core.data.datasource.firestore.doc.UserPreferencesDocument
import com.geoviksoft.turnia.core.data.datasource.firestorefunctions.requests.SharedCalendarResponse
import com.geoviksoft.turnia.core.data.datasource.firestorefunctions.requests.SharedGroupEventResponse
import com.geoviksoft.turnia.core.data.datasource.firestorefunctions.requests.SharedPersonalEventResponse
import com.geoviksoft.turnia.core.data.datasource.firestorefunctions.requests.SharedPersonalEventTypeResponse
import com.geoviksoft.turnia.core.data.datasource.firestorefunctions.requests.SharedPersonalOneOffEventResponse
import com.geoviksoft.turnia.core.domain.model.EventHistoryEntry
import com.geoviksoft.turnia.core.domain.model.EventId
import com.geoviksoft.turnia.core.domain.model.EventTypeId
import com.geoviksoft.turnia.core.domain.model.GroupEvent
import com.geoviksoft.turnia.core.domain.model.GroupEventType
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.PersonalEventType
import com.geoviksoft.turnia.core.domain.model.PersonalOneOffEvent
import com.geoviksoft.turnia.core.domain.model.PersonalTypedEvent
import com.geoviksoft.turnia.core.domain.model.SharedCalendar
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.system.parseEventDate
import com.geoviksoft.turnia.core.system.parseEventDateTime
import kotlinx.datetime.LocalDate

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
            groupEvents = response.groupEvents.mapNotNull {
                map(it, groupTypes, response.userNames)
            },
            personalEvents = response.personalEvents.mapNotNull { map(it, personalTypes) } +
                response.personalOneOffEvents.mapNotNull { map(it) },
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
        names: Map<String, String>,
    ): GroupEvent? {
        val type = types[event.groupId]?.get(EventTypeId(event.groupEventTypeId)) ?: return null

        return GroupEvent(
            id = EventId(event.eventId),
            groupId = GroupId(event.groupId),
            groupName = type.groupName,
            ownerId = UserId(event.ownerId),
            assigneeId = UserId(event.assigneeId),
            assigneeName = names[event.assigneeId].orEmpty(),
            type = type,
            date = LocalDate.parse(event.date),
            onSwap = event.onSwap,
            colorHex = type.color,
            history = event.holderUids.mapIndexed { index, uid ->
                EventHistoryEntry(
                    userId = UserId(uid),
                    userName = names[uid].orEmpty(),
                    returned = event.holderReturned.getOrElse(index) { false },
                )
            },
        )
    }

    private fun map(
        event: SharedPersonalEventResponse,
        types: Map<EventTypeId, PersonalEventType>,
    ): PersonalTypedEvent? {
        val type = event.personalEventTypeId?.let { types[EventTypeId(it)] } ?: return null

        return PersonalTypedEvent(
            id = EventId(event.eventId),
            type = type,
            date = parseEventDate(event.date) ?: return null,
            notes = event.notes.takeIf { it?.isNotEmpty() == true },
        )
    }

    private fun map(event: SharedPersonalOneOffEventResponse): PersonalOneOffEvent? = PersonalOneOffEvent(
        id = EventId(event.eventId),
        name = event.name,
        notes = event.notes.takeIf { it?.isNotEmpty() == true },
        start = parseEventDateTime(event.start) ?: return null,
        end = parseEventDateTime(event.end) ?: return null,
        allDay = event.allDay,
        color = event.color,
    )

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
