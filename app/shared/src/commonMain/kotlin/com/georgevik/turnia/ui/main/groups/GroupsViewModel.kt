package com.georgevik.turnia.ui.main.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.domain.model.GroupId
import com.georgevik.turnia.core.domain.model.JoinGroupError
import com.georgevik.turnia.core.domain.model.JoinGroupStatus
import com.georgevik.turnia.core.domain.model.JoinRequestStatus
import com.georgevik.turnia.core.domain.repository.GroupRepository
import com.georgevik.turnia.core.system.fold
import com.georgevik.turnia.core.system.valueOrEmpty
import com.georgevik.turnia.ui.main.groups.model.GroupRowUi
import com.georgevik.turnia.ui.main.groups.model.JoinRequestRowUi
import com.georgevik.turnia.ui.system.entityColor
import com.georgevik.turnia.ui.system.toComposeColorOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GroupsViewModel(
    private val groupRepository: GroupRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<GroupsUi>(GroupsUi.Loading)
    val uiState: StateFlow<GroupsUi> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            var first = true
            groupRepository.getGroups().collect { groups ->
                val rows = groups.map {
                    GroupRowUi(
                        id = it.id,
                        name = it.name,
                        color = it.color?.toComposeColorOrNull() ?: entityColor(it.id.value),
                        members = it.memberCount,
                        isAdmin = it.isAdmin,
                        isRevoked = it.isRevoked,
                    )
                }

                _uiState.update { state ->
                    val current = state as? GroupsUi.Success ?: GroupsUi.Success()
                    current.copy(groups = rows)
                }

                if (first || requests().isNotEmpty()) {
                    first = false
                    loadRequests()
                }
            }
        }
    }

    fun joinCodeChanged(code: String) =
        updateSuccess { it.copy(joinCode = code.uppercase()) }

    fun requestToJoin() {
        val code = (_uiState.value as? GroupsUi.Success)?.joinCode?.trim().orEmpty()
        if (code.isEmpty()) return

        updateSuccess { it.copy(joinInProgress = true) }
        viewModelScope.launch {
            groupRepository.requestToJoinGroup(code).fold(
                onSuccess = { status ->
                    updateSuccess {
                        it.copy(
                            joinCode = "",
                            joinInProgress = false,
                            userMessage = status.toMessage(),
                        )
                    }
                    if (status == JoinGroupStatus.Requested) loadRequests()
                },
                onFailure = { error ->
                    updateSuccess {
                        it.copy(
                            joinInProgress = false,
                            userMessage = error.toMessage()
                        )
                    }
                },
            )
        }
    }

    fun dismissRequest(groupId: GroupId) {
        viewModelScope.launch {
            updateSuccess { state ->
                state.copy(requests = state.requests.filterNot { it.groupId == groupId })
            }

            groupRepository.acknowledgeJoinRequest(groupId).fold(
                onSuccess = { },
                onFailure = {
                    updateSuccess { it.copy(userMessage = GroupsMessage.RequestDismissFailed) }
                    loadRequests()
                },
            )
        }
    }

    fun hideSnackbar() = updateSuccess { it.copy(userMessage = null) }

    private suspend fun loadRequests() {
        val requests = groupRepository.getMyJoinRequests().valueOrEmpty()
        requests.filter { it.status == JoinRequestStatus.ACCEPTED }
            .forEach { groupRepository.acknowledgeJoinRequest(it.groupId) }

        val rows = requests.mapNotNull { request ->
            when (request.status) {
                JoinRequestStatus.ACCEPTED -> null
                else -> JoinRequestRowUi(
                    groupId = request.groupId,
                    groupName = request.groupName,
                    isPending = request.status == JoinRequestStatus.PENDING,
                )
            }
        }
        updateSuccess { it.copy(requests = rows) }
    }

    private fun requests() = (_uiState.value as? GroupsUi.Success)?.requests.orEmpty()

    private fun JoinGroupStatus.toMessage() = when (this) {
        JoinGroupStatus.Joined -> GroupsMessage.Joined
        JoinGroupStatus.Requested -> GroupsMessage.JoinRequested
        JoinGroupStatus.AlreadyMember -> GroupsMessage.AlreadyMember
    }

    private fun JoinGroupError.toMessage() = when (this) {
        JoinGroupError.CodeNotFound -> GroupsMessage.JoinCodeNotFound
        JoinGroupError.InvitationInactive -> GroupsMessage.JoinInvitationInactive
        JoinGroupError.InvitationExpired -> GroupsMessage.JoinInvitationExpired
        JoinGroupError.RequestFailed -> GroupsMessage.JoinFailed
    }

    private fun updateSuccess(block: (GroupsUi.Success) -> GroupsUi.Success) =
        _uiState.update { state -> if (state is GroupsUi.Success) block(state) else state }
}
