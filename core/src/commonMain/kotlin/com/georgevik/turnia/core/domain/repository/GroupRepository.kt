package com.georgevik.turnia.core.domain.repository

import com.georgevik.turnia.core.domain.model.Group
import com.georgevik.turnia.core.domain.model.GroupError
import com.georgevik.turnia.core.domain.model.GroupEvent
import com.georgevik.turnia.core.system.Outcome
import kotlinx.datetime.LocalDate

interface GroupRepository {

    suspend fun getGroups() : List<Group>

    suspend fun addEvent(event: GroupEvent)

    suspend fun deleteEvent(eventId: String)

    suspend fun getEventsByGroup(
        groupId: String, date: LocalDate,
        monthDelta: Int = 1
    ): Result<List<GroupEvent>>

    suspend fun getEventsByUser(
        userId: String,
        date: LocalDate,
        monthDelta: Int = 1
    ): Result<List<GroupEvent>>

    suspend fun getGroup(groupId: String): Outcome<Group, GroupError>

    /** Creates the group when [group] has a blank id, updates it otherwise. */
    suspend fun saveGroup(group: Group): Outcome<Group, GroupError>

    suspend fun saveTypeColor(groupId: String, typeId: String, color: String) : Result<Unit>
}
