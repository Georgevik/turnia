package com.georgevik.turnia.ui.group.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.domain.model.Group
import com.georgevik.turnia.core.domain.model.GroupError
import com.georgevik.turnia.core.domain.model.GroupEventType
import com.georgevik.turnia.core.domain.model.GroupId
import com.georgevik.turnia.core.domain.model.GroupMember
import com.georgevik.turnia.core.domain.model.JoinRequest
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.domain.repository.GroupRepository
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.fold
import com.georgevik.turnia.core.system.valueOrEmpty
import com.georgevik.turnia.ui.group.detail.model.GroupDetailMessage
import com.georgevik.turnia.ui.group.detail.model.GroupDetailScreenError
import com.georgevik.turnia.ui.group.detail.model.GroupDetailUi
import com.georgevik.turnia.ui.group.detail.model.GroupDetailUi.GroupForm
import com.georgevik.turnia.ui.group.detail.model.GroupMemberUi
import com.georgevik.turnia.ui.group.detail.model.GroupTypeRowUi
import com.georgevik.turnia.ui.group.detail.model.JoinRequestUi
import com.georgevik.turnia.ui.system.entityColor
import com.georgevik.turnia.ui.system.toComposeColorOr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Group detail, edit and creation. A blank [groupId] means the screen is creating a group; the
 * loaded group's [Group.isAdmin] decides whether an existing one can be edited or is read-only.
 */
class GroupDetailViewModel(
    private val groupId: GroupId,
    private val groupRepository: GroupRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<GroupDetailUi>(GroupDetailUi.Loading)
    val uiState: StateFlow<GroupDetailUi> = _uiState.asStateFlow()

    /** Kept so saving can carry over the fields the form does not expose (the event types). */
    private var loadedGroup: Group? = null

    init {
        load()
    }

    fun retry() = load()

    /** Coming back from the event type detail: the type it just created has to show up here. */
    fun refresh() {
        // A group being created has nothing to re-read, and re-entering the initial state would
        // throw away the form.
        if (groupId.value.isBlank()) return

        load(showLoading = false)
    }

    private fun load(showLoading: Boolean = true) {
        if (groupId.value.isBlank()) {
            _uiState.update { newGroupState() }
            return
        }

        viewModelScope.launch {
            if (showLoading) _uiState.update { GroupDetailUi.Loading }

            groupRepository.getGroup(groupId).fold(
                onSuccess = { group ->
                    loadedGroup = group
                    // Only an admin can read the requests, so only an admin is asked for them.
                    val requests = if (group.isAdmin) {
                        groupRepository.getJoinRequests(groupId).valueOrEmpty()
                    } else {
                        emptyList()
                    }

                    _uiState.update { current ->
                        val loaded = group.toUiState(requests)
                        // A refresh brings the event types and the members up to date; whatever
                        // the user was typing is theirs and stays.
                        if (!showLoading && current is GroupDetailUi.Success) {
                            loaded.copy(form = current.form)
                        } else {
                            loaded
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update { GroupDetailUi.Error(error.toScreenError()) }
                },
            )
        }
    }

    fun onNameChanged(name: String) = updateForm { it.copy(name = name) }

    fun onAutoApproveChanged(autoApprove: Boolean) = updateForm {
        it.copy(autoApprove = autoApprove)
    }

    fun onMembersCanSeeCodeChanged(canSee: Boolean) = updateForm {
        it.copy(membersCanSeeCode = canSee)
    }

    /** The new code only reaches the group when the form is saved. */
    fun onRegenerateCode() = updateForm {
        it.copy(invitationCode = groupRepository.createInvitationCode(), codeChanged = true)
    }

    fun onSave() {
        val state = _uiState.value as? GroupDetailUi.Success ?: return
        val form = state.form
        if (!form.editable || form.name.isBlank()) return

        val name = form.name.trim()
        val group = loadedGroup?.copy(
            name = name,
            invitationCode = form.invitationCode.orEmpty(),
            autoApprove = form.autoApprove,
            membersCanSeeCode = form.membersCanSeeCode,
        ) ?: Group(
            id = GroupId(""),
            name = name,
            types = emptyList(),
            members = emptyList(),
            memberCount = 1,
            invitationCode = form.invitationCode.orEmpty(),
            autoApprove = form.autoApprove,
            membersCanSeeCode = form.membersCanSeeCode,
            isAdmin = true,
        )

        viewModelScope.launch {
            updateSuccess { it.copy(saving = true) }

            groupRepository.saveGroup(group).fold(
                onSuccess = { saved ->
                    loadedGroup = saved
                    updateSuccess { it.copy(saving = false, isSaved = true) }
                },
                onFailure = {
                    updateSuccess {
                        it.copy(saving = false, userMessage = GroupDetailMessage.SaveFailed)
                    }
                },
            )
        }
    }

    fun onAcceptRequest(userId: UserId) = answerRequest(userId) {
        groupRepository.acceptJoinRequest(groupId, userId)
    }

    fun onRejectRequest(userId: UserId) = answerRequest(userId) {
        groupRepository.rejectJoinRequest(groupId, userId)
    }

    /** The answered request leaves the list at once; the group is re-read for its new members. */
    private fun answerRequest(
        userId: UserId,
        answer: suspend () -> Outcome<Unit, GroupError>,
    ) {
        viewModelScope.launch {
            updateSuccess { state ->
                state.copy(joinRequests = state.joinRequests.filterNot { it.userId == userId })
            }

            answer().fold(
                onSuccess = { load(showLoading = false) },
                onFailure = {
                    updateSuccess { it.copy(userMessage = GroupDetailMessage.RequestFailed) }
                    load(showLoading = false)
                },
            )
        }
    }

    fun userMessageShown() = updateSuccess { it.copy(userMessage = null) }

    private fun newGroupState() = GroupDetailUi.Success(
        form = GroupForm(
            groupId = GroupId(""),
            name = "",
            memberCount = 1,
            invitationCode = groupRepository.createInvitationCode(),
            autoApprove = false,
            membersCanSeeCode = false,
            editable = true,
        ),
        eventTypes = emptyList(),
        members = emptyList(),
        joinRequests = emptyList(),
        isNew = true,
    )

    private fun Group.toUiState(requests: List<JoinRequest>) = GroupDetailUi.Success(
        form = GroupForm(
            groupId = id,
            name = name,
            memberCount = memberCount,
            invitationCode = invitationCode,
            autoApprove = autoApprove,
            membersCanSeeCode = membersCanSeeCode,
            editable = isAdmin,
        ),
        eventTypes = types.map { it.toUiRow() },
        members = members.map { it.toUiRow() },
        joinRequests = requests.map { JoinRequestUi(it.userId, it.name, it.username) },
        isNew = false,
    )

    private fun GroupMember.toUiRow() = GroupMemberUi(
        id = id,
        name = name,
        username = username,
        isAdmin = isAdmin,
    )

    private fun GroupEventType.toUiRow() = GroupTypeRowUi(
        typeId = id,
        groupId = groupId,
        name = name,
        acronym = acronym,
        startTime = startTime,
        endTime = endTime,
        color = color.toComposeColorOr(entityColor(id.value)),
    )

    private fun GroupError.toScreenError() = when (this) {
        GroupError.NotFound -> GroupDetailScreenError.NotFound
        GroupError.LoadFailed,
        GroupError.SaveFailed -> GroupDetailScreenError.LoadFailed
    }

    private fun updateForm(block: (GroupForm) -> GroupForm) =
        updateSuccess { it.copy(form = block(it.form)) }

    private fun updateSuccess(block: (GroupDetailUi.Success) -> GroupDetailUi.Success) =
        _uiState.update { if (it is GroupDetailUi.Success) block(it) else it }
}
