package com.georgevik.turnia.ui.main.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.domain.model.JoinGroupError
import com.georgevik.turnia.core.domain.model.JoinGroupStatus
import com.georgevik.turnia.core.domain.repository.GroupRepository
import com.georgevik.turnia.core.system.fold
import com.georgevik.turnia.ui.main.groups.model.GroupRowUi
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
            groupRepository.getGroups().collect { groups ->
                val rows = groups.map {
                    GroupRowUi(
                        id = it.id,
                        name = it.name,
                        members = it.memberCount,
                        isAdmin = it.isAdmin,
                    )
                }

                _uiState.update { state ->
                    val current = state as? GroupsUi.Success ?: GroupsUi.Success()
                    current.copy(groups = rows)
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

    fun hideSnackbar() = updateSuccess { it.copy(userMessage = null) }

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
