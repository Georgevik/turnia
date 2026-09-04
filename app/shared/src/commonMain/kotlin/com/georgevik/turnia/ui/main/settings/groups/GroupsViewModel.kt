package com.georgevik.turnia.ui.main.settings.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.domain.repository.GroupRepository
import com.georgevik.turnia.ui.main.group.calendarlist.model.GroupRowUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GroupsViewModel(private val groupRepository: GroupRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<GroupsUi>(GroupsUi.Loading)
    val uiState: StateFlow<GroupsUi> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /** A group created or left on the detail screen only shows up after this. */
    fun refresh() {
        viewModelScope.launch {
            val groups = groupRepository.getGroups()
                .map { GroupRowUi(id = it.id, name = it.name, members = it.memberCount) }

            _uiState.update { GroupsUi.Success(groups = groups) }
        }
    }

    fun userMessageShown() = _uiState.update { state ->
        if (state is GroupsUi.Success) state.copy(userMessage = null) else state
    }
}
