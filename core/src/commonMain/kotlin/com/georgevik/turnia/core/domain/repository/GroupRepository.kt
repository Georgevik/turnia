package com.georgevik.turnia.core.domain.repository

import com.georgevik.turnia.core.domain.model.Group
import com.georgevik.turnia.core.domain.model.GroupEvent
import com.georgevik.turnia.core.domain.model.GroupEventType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface GroupRepository {

    val groups: Flow<List<Group>>
    fun fetchGroupEvents(groupId: String): Flow<List<GroupEvent>>
    fun fetchCalendarEvents(userId: String): Flow<List<GroupEvent>>
    suspend fun fetchGroups()

    suspend fun getGroup(idGroup: String): Result<Group>

    suspend fun updateColor(groupId: String, color: String)
}
