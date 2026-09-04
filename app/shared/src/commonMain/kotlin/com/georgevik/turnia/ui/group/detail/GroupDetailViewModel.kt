package com.georgevik.turnia.ui.group.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.domain.model.Group
import com.georgevik.turnia.core.domain.model.GroupError
import com.georgevik.turnia.core.domain.model.GroupEventType
import com.georgevik.turnia.core.domain.model.GroupId
import com.georgevik.turnia.core.domain.repository.GroupRepository
import com.georgevik.turnia.core.system.fold
import com.georgevik.turnia.ui.group.detail.model.GroupDetailMessage
import com.georgevik.turnia.ui.group.detail.model.GroupDetailScreenError
import com.georgevik.turnia.ui.group.detail.model.GroupDetailUi
import com.georgevik.turnia.ui.group.detail.model.GroupDetailUi.GroupForm
import com.georgevik.turnia.ui.group.detail.model.GroupTypeRowUi
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

    private fun load() {
        if (groupId.value.isBlank()) {
            _uiState.update { newGroupState() }
            return
        }

        viewModelScope.launch {
            _uiState.update { GroupDetailUi.Loading }
            groupRepository.getGroup(groupId).fold(
                onSuccess = { group ->
                    loadedGroup = group
                    _uiState.update { group.toUiState() }
                },
                onFailure = { error ->
                    _uiState.update { GroupDetailUi.Error(error.toScreenError()) }
                },
            )
        }
    }

    fun onNameChanged(name: String) = updateSuccess { it.copy(form = it.form.copy(name = name)) }

    fun onSave() {
        val state = _uiState.value as? GroupDetailUi.Success ?: return
        if (!state.form.editable || state.form.name.isBlank()) return

        val name = state.form.name.trim()
        val group = loadedGroup?.copy(name = name) ?: Group(
            id = GroupId(""),
            name = name,
            types = emptyList(),
            memberCount = 1,
            invitationCode = null,
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

    fun userMessageShown() = updateSuccess { it.copy(userMessage = null) }

    private fun newGroupState() = GroupDetailUi.Success(
        form = GroupForm(
            groupId = GroupId(""),
            name = "",
            memberCount = 1,
            invitationCode = "",
            editable = true,
        ),
        eventTypes = emptyList(),
        isNew = true,
    )

    private fun Group.toUiState() = GroupDetailUi.Success(
        form = GroupForm(
            groupId = id,
            name = name,
            memberCount = memberCount,
            invitationCode = invitationCode.orEmpty(),
            editable = isAdmin,
        ),
        eventTypes = types.map { it.toUiRow() },
        isNew = false,
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

    private fun updateSuccess(block: (GroupDetailUi.Success) -> GroupDetailUi.Success) =
        _uiState.update { if (it is GroupDetailUi.Success) block(it) else it }
}
