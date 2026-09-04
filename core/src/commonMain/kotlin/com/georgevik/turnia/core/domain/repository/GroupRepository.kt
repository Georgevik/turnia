package com.georgevik.turnia.core.domain.repository

import com.georgevik.turnia.core.domain.model.EventId
import com.georgevik.turnia.core.domain.model.EventTypeId
import com.georgevik.turnia.core.domain.model.Group
import com.georgevik.turnia.core.domain.model.GroupError
import com.georgevik.turnia.core.domain.model.GroupEvent
import com.georgevik.turnia.core.domain.model.GroupId
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.system.Outcome
import kotlinx.datetime.LocalDate

interface GroupRepository {

    suspend fun getGroups() : List<Group>

    suspend fun addEvent(event: GroupEvent)

    suspend fun deleteEvent(eventId: EventId)

    suspend fun getEventsByGroup(
        groupId: GroupId, date: LocalDate,
        monthDelta: Int = 1
    ): Outcome<List<GroupEvent>, Unit>

    suspend fun getEventsByUser(
        userId: UserId,
        date: LocalDate,
        monthDelta: Int = 1
    ): Result<List<GroupEvent>>

    suspend fun getGroup(groupId: GroupId): Outcome<Group, GroupError>

    /** Creates the group when [group] has a blank id, updates it otherwise. */
    suspend fun saveGroup(group: Group): Outcome<Group, GroupError>

    suspend fun saveTypeColor(groupId: GroupId, typeId: EventTypeId, color: String) : Result<Unit>
}
