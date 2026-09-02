package com.georgevik.turnia.core.domain.repository

import com.georgevik.turnia.core.domain.model.Group
import com.georgevik.turnia.core.domain.model.GroupEvent
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

    suspend fun getGroup(idGroup: String): Result<Group>

    suspend fun updateColor(typeId: String, groupId: String, color: String) : Result<Unit>
}
