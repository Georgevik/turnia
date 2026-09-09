package com.georgevik.turnia.core.data.sharedcalendar

import com.georgevik.turnia.core.data.datasource.firestore.doc.UserDocument
import com.georgevik.turnia.core.data.datasource.firestorefunctions.SharedCalendarFunction
import com.georgevik.turnia.core.data.datasource.firestorefunctions.requests.SharedCalendarResponse
import com.georgevik.turnia.core.data.datasource.firestorefunctions.requests.SharedGroupEventResponse
import com.georgevik.turnia.core.data.datasource.firestorefunctions.requests.SharedPersonalEventResponse
import com.georgevik.turnia.core.data.datasource.firestorefunctions.requests.SharedPersonalEventTypeResponse
import com.georgevik.turnia.core.domain.model.EventId
import com.georgevik.turnia.core.domain.model.EventTypeId
import com.georgevik.turnia.core.domain.model.GroupEvent
import com.georgevik.turnia.core.domain.model.GroupEventType
import com.georgevik.turnia.core.domain.model.GroupId
import com.georgevik.turnia.core.domain.model.PersonalEvent
import com.georgevik.turnia.core.domain.model.PersonalEventType
import com.georgevik.turnia.core.domain.model.SharedCalendar
import com.georgevik.turnia.core.domain.model.SharedCalendarError
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.domain.repository.SharedCalendarRepository
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.map
import com.georgevik.turnia.core.system.toInstant
import kotlinx.datetime.LocalDate

class SharedCalendarRepositoryImpl(
    private val sharedCalendarFunction: SharedCalendarFunction,
) : SharedCalendarRepository {

    override suspend fun getSharedCalendar(
        ownerId: UserId,
        from: LocalDate,
        to: LocalDate,
    ): Outcome<SharedCalendar, SharedCalendarError> =
        sharedCalendarFunction.getSharedCalendar(ownerId, from, to).map { it.toDomain() }

    private fun SharedCalendarResponse.toDomain(): SharedCalendar {
        val types = groupEventTypes.mapValues { (groupId, types) ->
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
                        UserDocument.typeColorKey(groupId, type.id)
                    ],
                )
            }
        }

        val personalTypes = personalEventTypes.associate {
            EventTypeId(it.id) to it.toDomain()
        }

        return SharedCalendar(
            groupEvents = groupEvents.mapNotNull { it.toDomain(types) },
            personalEvents = personalEvents.mapNotNull { it.toDomain(personalTypes) },
        )
    }

    /** An event whose type the admin removed has nothing left to render. */
    private fun SharedGroupEventResponse.toDomain(
        types: Map<String, Map<EventTypeId, GroupEventType>>,
    ): GroupEvent? {
        val type = types[groupId]?.get(EventTypeId(groupEventTypeId)) ?: return null

        return GroupEvent(
            id = EventId(eventId),
            groupId = GroupId(groupId),
            groupName = type.groupName,
            ownerId = UserId(ownerId),
            assigneeId = UserId(assigneeId),
            assigneeName = "",
            type = type,
            date = LocalDate.parse(date),
            onSwap = onSwap,
            colorHex = type.color,
            history = emptyList(),
        )
    }

    private fun SharedPersonalEventResponse.toDomain(
        types: Map<EventTypeId, PersonalEventType>,
    ): PersonalEvent? {
        val type = personalEventTypeId?.let { types[EventTypeId(it)] } ?: return null

        return PersonalEvent(
            id = EventId(eventId),
            type = type,
            // Written as an instant, but an older event may carry a bare date.
            date = runCatching { kotlin.time.Instant.parse(date) }
                .getOrElse { LocalDate.parse(date).toInstant() },
            notes = notes.takeIf { it?.isNotEmpty() == true },
        )
    }

    private fun SharedPersonalEventTypeResponse.toDomain() = PersonalEventType(
        id = EventTypeId(id),
        name = name,
        color = color,
        acronym = acronym,
        description = description,
        startTime = startTime,
        endTime = endTime,
        isDeleted = isDeleted,
    )
}
