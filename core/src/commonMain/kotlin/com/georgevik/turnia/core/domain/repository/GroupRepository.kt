package com.georgevik.turnia.core.domain.repository

import com.georgevik.turnia.core.domain.model.Group
import com.georgevik.turnia.core.domain.model.GroupEvent
import com.georgevik.turnia.core.domain.model.GroupError
import com.georgevik.turnia.core.system.Outcome
import kotlinx.datetime.LocalDate

interface GroupRepository {

    suspend fun getGroups() : List<Group>

    suspend fun addGroupEvent(event: GroupEvent)

    suspend fun deleteGroupEvent(eventId: String)

    suspend fun retrieveGroupEvents(
        groupId: String, date: LocalDate,
        monthDelta: Int = 1
    ): Result<List<GroupEvent>>

    suspend fun retrieveCalendarEvents(
        userId: String,
        date: LocalDate,
        monthDelta: Int = 1
    ): Result<List<GroupEvent>>

    suspend fun getGroup(idGroup: String): Outcome<Group, GroupError>

    /** Creates the group when [group] has a blank id, updates it otherwise. */
    suspend fun saveGroup(group: Group): Outcome<Group, GroupError>

    suspend fun updateColor(typeId: String, groupId: String, color: String) : Result<Unit>
}
