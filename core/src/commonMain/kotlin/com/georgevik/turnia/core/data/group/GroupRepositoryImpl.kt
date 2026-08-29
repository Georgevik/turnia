package com.georgevik.turnia.core.data.group

import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.Group
import com.georgevik.turnia.core.domain.model.GroupEvent
import com.georgevik.turnia.core.domain.repository.GroupRepository
import com.georgevik.turnia.core.system.MOCK_TYPES
import com.georgevik.turnia.core.system.mockColor
import com.georgevik.turnia.core.system.mockDate
import com.georgevik.turnia.core.system.mockGroupName
import com.georgevik.turnia.core.system.mockRealName
import com.georgevik.turnia.core.system.mockUuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.random.Random
import kotlin.uuid.Uuid

@OptIn(ExperimentalAtomicApi::class)
class GroupRepositoryImpl : GroupRepository {

    private val _groups = MutableStateFlow(emptyList<Group>())
    override val groups: Flow<List<Group>> = _groups

    override suspend fun fetchGroups() {
        val groups = (1..3).map {
            Group(
                id = mockUuid(),
                name = mockGroupName(),
                types = MOCK_TYPES
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

    override fun fetchGroupEvents(groupId: String): Flow<List<GroupEvent>> = flow {
        // TODO Call DataSource
        val events = (0 until 200).map {
            val ownerId = mockUuid()

            GroupEvent(
                id = mockUuid(),
                groupId = groupId,
                ownerId = ownerId,
                assigneeId = if (Random.nextBoolean()) ownerId else mockUuid(),
                type = MOCK_TYPES.random(),
                date = mockDate(),
                onSwap = Random.nextInt(7) == 1,
                colorHex = mockColor(),
                history = (0..(0..3).random()).map { mockRealName() })
        }

        if (events.isEmpty()) {
            Logger.w(TAG, "Group $groupId not found")
        } else {
            emit(events)
        }
    }

    override fun fetchCalendarEvents(userId: String): Flow<List<GroupEvent>> {
        // TODO("Not yet implemented")
        return MutableStateFlow(emptyList())
    }

    companion object {
        private const val TAG = "GroupRepository"
    }
}
