package com.geoviksoft.turnia.ui.main.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoviksoft.turnia.core.domain.model.JoinGroupError
import com.geoviksoft.turnia.core.domain.model.JoinGroupStatus
import com.geoviksoft.turnia.core.domain.model.JoinRequestStatus
import com.geoviksoft.turnia.core.domain.repository.GroupRepository
import com.geoviksoft.turnia.core.domain.repository.InvitationLinkRepository
import com.geoviksoft.turnia.core.system.fold
import com.geoviksoft.turnia.ui.main.groups.model.GroupRowUi
import com.geoviksoft.turnia.ui.main.groups.model.GroupsFilter
import com.geoviksoft.turnia.ui.main.groups.model.JoinRequestRowUi
import com.geoviksoft.turnia.ui.main.groups.model.toRowUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GroupsViewModel(
    private val groupRepository: GroupRepository,
    private val invitationLinkRepository: InvitationLinkRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<GroupsUi>(GroupsUi.Loading)
    val uiState: StateFlow<GroupsUi> = _uiState.asStateFlow()

    /** The code of an invitation link the user opened, until this tab puts it in the join sheet. */
    val pendingJoinCode: StateFlow<String?> = invitationLinkRepository.pendingCode

    private val filter = MutableStateFlow(GroupsFilter.ALL)

    init {
        viewModelScope.launch {
            combine(pendingRequests(), groups(), filter) { requests, groups, selected ->
                val showFilters = groups.isNotEmpty() && requests.isNotEmpty()
                if (!showFilters) filter.value = GroupsFilter.ALL

                val active = if (showFilters) selected else GroupsFilter.ALL
                _uiState.update { state ->
                    val current = state as? GroupsUi.Success ?: GroupsUi.Success()
                    current.copy(
                        groups = when (active) {
                            GroupsFilter.ALL, GroupsFilter.MINE -> groups
                            GroupsFilter.PENDING -> emptyList()
                        },
                        requests = when (active) {
                            GroupsFilter.ALL, GroupsFilter.PENDING -> requests
                            GroupsFilter.MINE -> emptyList()
                        },
                        filter = active,
                        pendingCount = requests.size,
                        showFilters = showFilters,
                        isEmpty = groups.isEmpty() && requests.isEmpty(),
                    )
                }
            }.collect {}
        }
    }

    fun filterSelected(filter: GroupsFilter) {
        this.filter.value = filter
    }

    private suspend fun pendingRequests(): Flow<List<JoinRequestRowUi>> =
        groupRepository.getMyJoinRequests()
            .map { requests ->
                requests.mapNotNull { request ->
                    when (request.status) {
                        JoinRequestStatus.REJECTED -> null
                        JoinRequestStatus.ACCEPTED -> null
                        JoinRequestStatus.PENDING -> JoinRequestRowUi(
                            groupId = request.groupId,
                            groupName = request.groupName,
                            isPending = true,
                        )
                    }
                }
            }

    private fun groups(): Flow<List<GroupRowUi>> =
        groupRepository.getGroups().map { groups -> groups.filterNot { it.isRevoked }.map { it.toRowUi() } }

    fun joinCodeChanged(code: String) =
        updateSuccess { it.copy(joinCode = code.uppercase()) }

    fun joinCodeReceived(code: String) {
        joinCodeChanged(code)
        invitationLinkRepository.codeHandled()
    }

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
