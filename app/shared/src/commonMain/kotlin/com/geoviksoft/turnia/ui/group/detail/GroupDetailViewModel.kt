package com.geoviksoft.turnia.ui.group.detail

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoviksoft.turnia.core.domain.analytics.Analytics
import com.geoviksoft.turnia.core.domain.analytics.AnalyticsEvent
import com.geoviksoft.turnia.core.domain.model.Group
import com.geoviksoft.turnia.core.domain.model.EventTypeId
import com.geoviksoft.turnia.core.domain.model.GroupError
import com.geoviksoft.turnia.core.domain.model.GroupEventType
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.GroupMember
import com.geoviksoft.turnia.core.domain.model.JoinRequest
import com.geoviksoft.turnia.core.domain.model.NewGroup
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.domain.model.UserProfile
import com.geoviksoft.turnia.core.domain.repository.GroupRepository
import com.geoviksoft.turnia.core.domain.repository.UserRepository
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.fold
import com.geoviksoft.turnia.core.system.onFailure
import com.geoviksoft.turnia.core.system.valueOrNull
import com.geoviksoft.turnia.core.system.valueOrEmpty
import com.geoviksoft.turnia.ui.group.detail.model.GroupCloseUi
import com.geoviksoft.turnia.ui.group.detail.model.GroupDetailMessage
import com.geoviksoft.turnia.ui.group.detail.model.GroupDetailScreenError
import com.geoviksoft.turnia.ui.group.detail.model.GroupDetailUi
import com.geoviksoft.turnia.ui.group.detail.model.GroupDetailUi.GroupForm
import com.geoviksoft.turnia.ui.group.detail.model.GroupMemberUi
import com.geoviksoft.turnia.ui.group.detail.model.GroupTypeRowUi
import com.geoviksoft.turnia.ui.group.detail.model.JoinRequestUi
import com.geoviksoft.turnia.ui.system.color.entityColor
import com.geoviksoft.turnia.ui.system.color.toComposeColorOr
import com.geoviksoft.turnia.ui.system.color.toComposeColorOrNull
import com.geoviksoft.turnia.ui.system.color.toHex
import com.geoviksoft.turnia.ui.shiftsetup.model.ShiftPreset
import com.geoviksoft.turnia.ui.system.components.time.toTimeOrNull
import com.geoviksoft.turnia.ui.system.createUuid
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Group detail, edit and creation. A null [groupId] means the screen is creating a group; the
 * loaded group's [Group.isAdmin] decides whether an existing one can be edited or is read-only.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GroupDetailViewModel(
    private val groupId: GroupId?,
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val analytics: Analytics,
) : ViewModel() {

    private val _uiState = MutableStateFlow<GroupDetailUi>(GroupDetailUi.Loading)
    val uiState: StateFlow<GroupDetailUi> = _uiState.asStateFlow()

    /** Kept so saving can carry over the fields the form does not expose (the event types). */
    private var loadedGroup: Group? = null

    /** The form as the group last filled it: while the user has not touched it, a change replaces it. */
    private var loadedForm: GroupForm? = null

    private var observation: Job? = null

    /** A new group is proposed its types once, not again on every return from a type's screen. */
    private var typesProposed = false

    init {
        // Whatever a creation abandoned halfway left behind is not this group's.
        if (groupId == null) groupRepository.consumePendingEventTypes()
        load()
        observePendingTypes()
    }

    /**
     * A type created while the group does not exist has nowhere to be written, so it waits on
     * the repository and reaches the list from there instead of from a reload.
     */
    private fun observePendingTypes() {
        if (groupId != null) return

        viewModelScope.launch {
            groupRepository.pendingEventTypes.collect { types ->
                updateSuccess { state -> state.copy(eventTypes = types.map { it.toUiRow() }) }
            }
        }
    }

    fun retry() = load()

    /**
     * Seeds a new group with the usual shifts, so naming it is enough to create it. [texts] holds
     * each preset's name and acronym as the screen resolved them, in the language on screen: from
     * here on they are ordinary pending types, edited and saved like any other.
     */
    fun proposeTypes(texts: Map<ShiftPreset, Pair<String, String>>) {
        if (groupId != null || typesProposed) return
        typesProposed = true

        ShiftPreset.groupDefaults.forEach { preset ->
            val (name, acronym) = texts[preset] ?: return@forEach
            groupRepository.setPendingEventType(
                GroupEventType(
                    id = EventTypeId(createUuid()),
                    groupId = GroupId(""),
                    groupName = "",
                    name = name,
                    acronym = acronym,
                    description = null,
                    startTime = preset.start.toString().toTimeOrNull(),
                    endTime = preset.end.toString().toTimeOrNull(),
                    swappable = true,
                    defaultColor = preset.color.toHex(),
                    userColor = null,
                )
            )
        }
    }

    /** Only a group still being created: an existing group's types are not removed from here. */
    fun onRemoveType(typeId: EventTypeId) {
        if (groupId != null) return
        groupRepository.removePendingEventType(typeId)
    }

    /**
     * Follows the group rather than reading it once: a type saved on its own screen, a request
     * answered, a member removed or another admin's edit all arrive here through the group's sync
     * listener, from the cache unless the server moved on — so nothing has to reload on resume.
     */
    private fun load(showLoading: Boolean = true) {
        if (groupId == null) {
            _uiState.update { newGroupState() }
            return
        }

        observation?.cancel()
        if (showLoading) _uiState.update { GroupDetailUi.Loading }
        observation = viewModelScope.launch {
            groupRepository.observeGroup(groupId)
                .flatMapLatest { outcome ->
                    // Only an admin can read the requests, so only an admin is asked for them.
                    if (outcome.valueOrNull()?.isAdmin == true) {
                        groupRepository.observeJoinRequests(groupId).map { outcome to it }
                    } else {
                        flowOf(outcome to emptyList())
                    }
                }
                .collect { (outcome, requests) ->
                    outcome.fold(
                        onSuccess = { group -> show(group, requests) },
                        onFailure = { error ->
                            _uiState.update { GroupDetailUi.Error(error.toScreenError()) }
                        },
                    )
                }
        }
    }

    private suspend fun show(group: Group, requests: List<JoinRequest>) {
        loadedGroup = group
        val loaded = group.toUiState(requests, avatarsOf(group, requests))
        val previousForm = loadedForm
        loadedForm = loaded.form

        _uiState.update { current ->
            if (current !is GroupDetailUi.Success) return@update loaded
            current.copy(
                // Whatever the user was typing is theirs and stays; an untouched form follows the group.
                form = if (current.form == previousForm) loaded.form else current.form,
                eventTypes = loaded.eventTypes,
                members = loaded.members,
                joinRequests = loaded.joinRequests,
            )
        }
    }

    fun onNameChanged(name: String) = updateForm { it.copy(name = name) }

    fun onPickColor(color: Color) = updateForm { it.copy(color = color) }

    fun onAutoApproveChanged(autoApprove: Boolean) = updateForm {
        it.copy(autoApprove = autoApprove)
    }

    /** The new code only reaches the group when the form is saved. */
    fun onRegenerateCode() = updateForm {
        it.copy(invitationCode = groupRepository.createInvitationCode(), codeChanged = true)
    }

    fun onSave() {
        val state = _uiState.value as? GroupDetailUi.Success ?: return
        val form = state.form
        if (!form.editable || form.name.isBlank()) return
        // A group nobody can add a shift to is not a group; the button is disabled, this is the rule.
        if (groupId == null && groupRepository.pendingEventTypes.value.isEmpty()) return

        val name = form.name.trim()
        val loaded = loadedGroup

        viewModelScope.launch {
            updateSuccess { it.copy(saving = true) }

            val outcome = if (loaded == null) {
                // The whole group is one document, types included, so a group is created with its
                // types in a single write and never exists without them.
                groupRepository.createGroup(
                    NewGroup(
                        name = name,
                        color = form.color.toHex(),
                        types = groupRepository.pendingEventTypes.value,
                        invitationCode = form.invitationCode.orEmpty(),
                        autoApprove = form.autoApprove,
                    )
                )
            } else {
                groupRepository.updateGroup(
                    loaded.copy(
                        name = name,
                        color = form.color.toHex(),
                        invitationCode = form.invitationCode.orEmpty(),
                        autoApprove = form.autoApprove,
                    )
                )
            }

            outcome.fold(
                onSuccess = { saved ->
                    loadedGroup = saved
                    // Written now, and only now: a failed save has to leave them on screen.
                    groupRepository.consumePendingEventTypes()
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

    fun onAcceptRequest(userId: UserId) {
        val groupId = groupId ?: return
        answerRequest(userId) { groupRepository.acceptJoinRequest(groupId, userId) }
    }

    fun onRejectRequest(userId: UserId) {
        val groupId = groupId ?: return
        answerRequest(userId) { groupRepository.rejectJoinRequest(groupId, userId) }
    }

    /**
     * Removes a member. Whatever they still hold stays on the group's calendar — the shifts still
     * need covering — and only they can see it from then on.
     */
    fun onRemoveMember(userId: UserId) {
        val groupId = groupId ?: return

        viewModelScope.launch {
            updateSuccess { state ->
                state.copy(members = state.members.filterNot { it.id == userId })
            }

            // Success needs nothing more: the group follows the function's write on its own.
            groupRepository.removeMember(groupId, userId).onFailure {
                updateSuccess { it.copy(userMessage = GroupDetailMessage.RemoveMemberFailed) }
                // Nothing moved on the server, so no emission will bring the member back.
                load(showLoading = false)
            }
        }
    }

    fun onInvitationShared() = analytics.log(AnalyticsEvent.GroupInviteShared)

    fun onLeaveGroup() {
        val groupId = groupId ?: return
        closeGroup(
            action = { groupRepository.leaveGroup(groupId) },
            message = { error ->
                if (error == GroupError.LastAdmin) GroupDetailMessage.LeaveLastAdmin
                else GroupDetailMessage.LeaveFailed
            },
        )
    }

    fun onDeleteGroup() {
        val groupId = groupId ?: return
        closeGroup(
            action = { groupRepository.deleteGroup(groupId) },
            message = { error ->
                if (error == GroupError.NotEmpty) GroupDetailMessage.DeleteNotEmpty
                else GroupDetailMessage.DeleteFailed
            },
        )
    }

    /**
     * The confirmation dialog stays up for the whole operation, so the outcome only takes effect
     * once the UI has shown it back — see [closeResultShown].
     */
    private fun closeGroup(
        action: suspend () -> Outcome<Unit, GroupError>,
        message: (GroupError) -> GroupDetailMessage,
    ) {
        viewModelScope.launch {
            updateSuccess { it.copy(close = GroupCloseUi.Running) }

            action().fold(
                onSuccess = { updateSuccess { it.copy(close = GroupCloseUi.Succeeded) } },
                onFailure = { error ->
                    updateSuccess { it.copy(close = GroupCloseUi.Failed(message(error))) }
                },
            )
        }
    }

    fun closeResultShown() = updateSuccess { state ->
        when (val close = state.close) {
            GroupCloseUi.Succeeded -> state.copy(close = null, hasLeft = true)
            is GroupCloseUi.Failed -> state.copy(close = null, userMessage = close.message)
            GroupCloseUi.Running, null -> state
        }
    }

    /** The answered request leaves the list at once; the group's new members arrive on their own. */
    private fun answerRequest(
        userId: UserId,
        answer: suspend () -> Outcome<Unit, GroupError>,
    ) {
        viewModelScope.launch {
            updateSuccess { state ->
                state.copy(joinRequests = state.joinRequests.filterNot { it.userId == userId })
            }

            answer().onFailure {
                updateSuccess { it.copy(userMessage = GroupDetailMessage.RequestFailed) }
                // Nothing moved on the server, so no emission will bring the request back.
                load(showLoading = false)
            }
        }
    }

    fun userMessageShown() = updateSuccess { it.copy(userMessage = null) }

    private fun newGroupState() = GroupDetailUi.Success(
        form = GroupForm(
            groupId = null,
            name = "",
            color = entityColor(createUuid()),
            memberCount = 1,
            invitationCode = groupRepository.createInvitationCode(),
            autoApprove = false,
            editable = true,
        ),
        eventTypes = emptyList(),
        members = emptyList(),
        joinRequests = emptyList(),
        isNew = true,
    )

    /**
     * The group document carries every member's name but not their avatar, which lives on their own
     * profile — one read each, and the cache answers from the second open on.
     */
    private suspend fun avatarsOf(
        group: Group,
        requests: List<JoinRequest>,
    ): Map<UserId, UserProfile.AnimalAvatar> {
        val uids = (group.members.map { it.id } + requests.map { it.userId }).distinct()
        if (uids.isEmpty()) return emptyMap()

        return userRepository.getProfiles(uids).valueOrEmpty()
            .associate { it.id to it.avatar }
    }

    private fun Group.toUiState(
        requests: List<JoinRequest>,
        avatars: Map<UserId, UserProfile.AnimalAvatar>,
    ) = GroupDetailUi.Success(
        form = GroupForm(
            groupId = id,
            name = name,
            color = color?.toComposeColorOrNull() ?: entityColor(id.value),
            memberCount = memberCount,
            invitationCode = invitationCode,
            autoApprove = autoApprove,
            editable = isAdmin,
        ),
        eventTypes = types.map { it.toUiRow() },
        members = members.map { it.toUiRow(avatars) },
        joinRequests = requests.map {
            JoinRequestUi(it.userId, it.name, it.username, avatars.avatarOf(it.userId))
        },
        isNew = false,
    )

    private fun GroupMember.toUiRow(avatars: Map<UserId, UserProfile.AnimalAvatar>) = GroupMemberUi(
        id = id,
        name = name,
        username = username,
        isAdmin = isAdmin,
        avatar = avatars.avatarOf(id),
    )

    private fun Map<UserId, UserProfile.AnimalAvatar>.avatarOf(id: UserId) =
        this[id] ?: UserProfile.AnimalAvatar.NONE

    private fun GroupEventType.toUiRow() = GroupTypeRowUi(
        typeId = id,
        // The screen's own group, so a type still waiting for one carries none.
        groupId = this@GroupDetailViewModel.groupId,
        name = name,
        acronym = acronym,
        startTime = startTime,
        endTime = endTime,
        color = color.toComposeColorOr(entityColor(id.value)),
    )

    private fun GroupError.toScreenError() = when (this) {
        GroupError.NotFound -> GroupDetailScreenError.NotFound
        GroupError.LoadFailed,
        GroupError.SaveFailed,
        GroupError.LastAdmin,
        GroupError.NotEmpty -> GroupDetailScreenError.LoadFailed
    }

    private fun updateForm(block: (GroupForm) -> GroupForm) =
        updateSuccess { it.copy(form = block(it.form)) }

    private fun updateSuccess(block: (GroupDetailUi.Success) -> GroupDetailUi.Success) =
        _uiState.update { if (it is GroupDetailUi.Success) block(it) else it }
}
