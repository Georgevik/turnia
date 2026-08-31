package com.georgevik.turnia.core.data.group

import com.georgevik.turnia.core.domain.model.Group
import com.georgevik.turnia.core.domain.model.GroupEvent
import com.georgevik.turnia.core.domain.repository.GroupRepository
import com.georgevik.turnia.core.system.MOCK_GROUP_TYPES
import com.georgevik.turnia.core.system.mockGroupEvent
import com.georgevik.turnia.core.system.mockGroupName
import com.georgevik.turnia.core.system.mockUuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
class GroupRepositoryImpl : GroupRepository {

    private val _mockEvents = mutableMapOf<String, List<GroupEvent>>()

    private val _groups = MutableStateFlow(emptyList<Group>())
    override val groups: Flow<List<Group>> = _groups

    override suspend fun fetchGroups() {
        val groups = (1..3).map {
            Group(
                id = mockUuid(),
                name = mockGroupName(),
                types = MOCK_GROUP_TYPES
            )
        }

        _groups.emit(groups)
    }

    override suspend fun getGroup(idGroup: String): Result<Group> {
        val group = _groups.value.find { it.id == idGroup }

        return if (group != null) Result.success(group)
        else Result.failure(Exception("Group not found"))
    }

    override suspend fun updateColor(groupId: String, color: String) {
        TODO("Not yet implemented")
    }

    override suspend fun retrieveGroupEvents(
        groupId: String,
        date: LocalDate,
        monthDelta: Int
    ): Result<List<GroupEvent>> {
        // TODO Call DataSource
        val events = buildList {
            addAll(getEventsPerDate(groupId, date))

            (1..monthDelta).forEach { delta ->
                addAll(getEventsPerDate(groupId, date.plus(delta, DateTimeUnit.MONTH)))
                addAll(getEventsPerDate(groupId, date.minus(delta, DateTimeUnit.MONTH)))
            }
        }.distinctBy { it.id }

        return Result.success(events)
    }

    override suspend fun retrieveCalendarEvents(
        userId: String,
        date: LocalDate,
        monthDelta: Int
    ): Result<List<GroupEvent>> {
        val events = buildList {
            addAll(getEventsPerDate(userId, date))

            (1..monthDelta).forEach { delta ->
                addAll(getEventsPerDate(userId, date.plus(delta, DateTimeUnit.MONTH)))
                addAll(getEventsPerDate(userId, date.minus(delta, DateTimeUnit.MONTH)))
            }
        }.distinctBy { it.id }

        return Result.success(events)
    }


    private fun getEventsPerDate(userId: String, date: LocalDate): List<GroupEvent> {
        return _mockEvents.getOrPut("${userId}_${date.year}_${date.month}") {
            mockGroupEvent(userId, date)
        }
    }

    companion object {
        private const val TAG = "GroupRepository"
    }
}
