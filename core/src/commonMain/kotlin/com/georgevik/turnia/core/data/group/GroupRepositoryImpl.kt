package com.georgevik.turnia.core.data.group

import com.georgevik.turnia.core.domain.model.EventId
import com.georgevik.turnia.core.domain.model.EventTypeId
import com.georgevik.turnia.core.domain.model.Group
import com.georgevik.turnia.core.domain.model.GroupError
import com.georgevik.turnia.core.domain.model.GroupEvent
import com.georgevik.turnia.core.domain.model.GroupId
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.domain.repository.GroupRepository
import com.georgevik.turnia.core.domain.repository.UserRepository
import com.georgevik.turnia.core.system.MOCK_GROUPS
import com.georgevik.turnia.core.system.MOCK_MY_ID
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.mockDelay
import com.georgevik.turnia.core.system.mockGenerateEvents
import com.georgevik.turnia.core.system.mockUuid
import com.georgevik.turnia.core.system.toFailure
import com.georgevik.turnia.core.system.toSuccess
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.random.Random

@OptIn(ExperimentalAtomicApi::class)
class GroupRepositoryImpl(
    private val userRepository: UserRepository,
) : GroupRepository {

    private val _mockEvents = mutableMapOf<String, List<GroupEvent>>()

    /** Groups created or edited in this session, keyed by id; they shadow [MOCK_GROUPS]. */
    private val _mockSavedGroups = linkedMapOf<GroupId, Group>()

    override suspend fun addEvent(event: GroupEvent) {
        mockDelay()
        val existing = getEventsPerDate(event.date)
        _mockEvents[bucketKey(event.date)] = existing + event
    }

    override suspend fun deleteEvent(eventId: EventId) {
        // TODO use data source
        mockDelay()
        _mockEvents.keys.toList().forEach { key ->
            _mockEvents[key] = _mockEvents.getValue(key).filterNot { it.id == eventId }
        }
    }

    override suspend fun getGroups(): List<Group> {
        mockDelay()
        val known = MOCK_GROUPS.map { _mockSavedGroups[it.id] ?: it }
        val created = _mockSavedGroups.values.filterNot { saved ->
            MOCK_GROUPS.any { it.id == saved.id }
        }
        return known + created
    }

    override suspend fun getGroup(groupId: GroupId): Outcome<Group, GroupError> {
        mockDelay()
        val group = getGroups().find { it.id == groupId }

        return group?.toSuccess() ?: GroupError.NotFound.toFailure()
    }

    override suspend fun saveGroup(group: Group): Outcome<Group, GroupError> {
        mockDelay()
        val saved = if (group.id.value.isBlank()) {
            group.copy(id = GroupId(mockUuid()), invitationCode = mockUuid().take(6).uppercase(), isAdmin = true)
        } else {
            group
        }
        _mockSavedGroups[saved.id] = saved
        return saved.toSuccess()
    }

    override suspend fun saveTypeColor(groupId: GroupId, typeId: EventTypeId, color: String): Result<Unit> {
        mockDelay()
        return if (Random.nextBoolean()) {
            Result.success(Unit)
        } else {
            Result.failure(Throwable())
        }
    }

    override suspend fun getEventsByGroup(
        groupId: GroupId,
        date: LocalDate,
        monthDelta: Int
    ): Outcome<List<GroupEvent>, Unit> {
        mockDelay()
        // TODO Call DataSource
        val events = buildList {
            addAll(getEventsPerDate(date))

            (1..monthDelta).forEach { delta ->
                addAll(getEventsPerDate(date.plus(delta, DateTimeUnit.MONTH)))
                addAll(getEventsPerDate(date.minus(delta, DateTimeUnit.MONTH)))
            }
        }.distinctBy { it.id }.filter { it.groupId == groupId }

        return events.toSuccess()
    }

    override suspend fun getEventsByUser(
        userId: UserId,
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
        val me = userRepository.loggedUser?.id ?: MOCK_MY_ID
        return _mockEvents.getOrPut("${date.year}_${date.month}") {
            mockGenerateEvents(date, me)
        }
    }


    private fun bucketKey(date: LocalDate) = "${date.year}_${date.month}"

    companion object {
        private const val TAG = "GroupRepository"
    }
}
