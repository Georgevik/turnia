package com.georgevik.turnia.core.domain.repository

import com.georgevik.turnia.core.domain.model.Group
import com.georgevik.turnia.core.domain.model.GroupEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface GroupRepository {

    val groups: Flow<List<Group>>

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

    suspend fun fetchGroups()

    suspend fun getGroup(idGroup: String): Result<Group>

    suspend fun updateColor(typeId: String, groupId: String, color: String)
}
