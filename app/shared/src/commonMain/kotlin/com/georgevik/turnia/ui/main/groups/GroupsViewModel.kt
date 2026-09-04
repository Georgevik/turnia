package com.georgevik.turnia.ui.main.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.domain.repository.GroupRepository
import com.georgevik.turnia.ui.main.groups.model.GroupRowUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GroupsViewModel(private val groupRepository: GroupRepository) : ViewModel() {

    private var allGroups: List<GroupRowUi> = emptyList()

    private val _uiState = MutableStateFlow<GroupsUi>(GroupsUi.Loading)
    val uiState: StateFlow<GroupsUi> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            groupRepository.getGroups().collect { groups ->
                allGroups = groups.map {
                    GroupRowUi(
                        id = it.id,
                        name = it.name,
                        members = it.memberCount,
                        isAdmin = it.isAdmin,
                    )
                }

                val query = (_uiState.value as? GroupsUi.Success)?.query.orEmpty()
                _uiState.value = GroupsUi.Success(
                    query = query,
                    groups = allGroups.filter { it.name.contains(query, ignoreCase = true) },
                )
            }
        }
    }

    fun searchBy(query: String) = updateSuccess { state ->
        state.copy(
            query = query,
            groups = allGroups.filter { it.name.contains(query, ignoreCase = true) },
        )
    }

    fun userMessageShown() = updateSuccess { it.copy(userMessage = null) }

    private fun updateSuccess(block: (GroupsUi.Success) -> GroupsUi.Success) =
        _uiState.update { state -> if (state is GroupsUi.Success) block(state) else state }
}
