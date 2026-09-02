package com.georgevik.turnia.core.data.group

import com.georgevik.turnia.core.domain.model.Group
import com.georgevik.turnia.core.domain.model.GroupEvent
import com.georgevik.turnia.core.domain.repository.GroupRepository
import com.georgevik.turnia.core.system.mockDelay
import com.georgevik.turnia.core.system.MOCK_GROUPS
import com.georgevik.turnia.core.system.mockGenerateEvents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.random.Random

@OptIn(ExperimentalAtomicApi::class)
class GroupRepositoryImpl : GroupRepository {

    private val _mockEvents = mutableMapOf<String, List<GroupEvent>>()

    override suspend fun addGroupEvent(event: GroupEvent) {
        mockDelay()
        val existing = getEventsPerDate(event.date)
        _mockEvents[bucketKey(event.date)] = existing + event
    }

    override suspend fun deleteGroupEvent(eventId: String) {
        // TODO use data source
        mockDelay()
        _mockEvents.keys.toList().forEach { key ->
            _mockEvents[key] = _mockEvents.getValue(key).filterNot { it.id == eventId }
        }
    }

    override suspend fun getGroups(): List<Group> {
        mockDelay()
        return MOCK_GROUPS
    }

    override suspend fun getGroup(idGroup: String): Result<Group> {
        mockDelay()
        val group = getGroups().find { it.id == idGroup }

        return if (group != null) Result.success(group)
        else Result.failure(Exception("Group not found"))
    }

    override suspend fun updateColor(typeId: String, groupId: String, color: String): Result<Unit> {
        mockDelay()
        return if (Random.nextBoolean()) {
            Result.success(Unit)
        } else {
            Result.failure(Throwable())
        }
    }

    override suspend fun retrieveGroupEvents(
        groupId: String,
        date: LocalDate,
        monthDelta: Int
    ): Result<List<GroupEvent>> {
        mockDelay()
        // TODO Call DataSource
        val events = buildList {
            addAll(getEventsPerDate(date))

            (1..monthDelta).forEach { delta ->
                addAll(getEventsPerDate(date.plus(delta, DateTimeUnit.MONTH)))
                addAll(getEventsPerDate(date.minus(delta, DateTimeUnit.MONTH)))
            }
        }.distinctBy { it.id }.filter { it.groupId == groupId }

        return Result.success(events)
    }

    override suspend fun retrieveCalendarEvents(
        userId: String,
        date: LocalDate,
        monthDelta: Int
    ): Result<List<GroupEvent>> {
        val events = buildList {
            addAll(getEventsPerDate(date))

            (1..monthDelta).forEach { delta ->
                addAll(getEventsPerDate(date.plus(delta, DateTimeUnit.MONTH)))
                addAll(getEventsPerDate(date.minus(delta, DateTimeUnit.MONTH)))
            }
        }.distinctBy { it.id }.filter { it.ownerId == userId || it.assigneeId == userId }

        return Result.success(events)
    }


    private fun getEventsPerDate(date: LocalDate): List<GroupEvent> {
        return _mockEvents.getOrPut("${date.year}_${date.month}") {
            mockGenerateEvents(date)
        }
    }


    private fun bucketKey(date: LocalDate) = "${date.year}_${date.month}"

    companion object {
        private const val TAG = "GroupRepository"
    }
}
