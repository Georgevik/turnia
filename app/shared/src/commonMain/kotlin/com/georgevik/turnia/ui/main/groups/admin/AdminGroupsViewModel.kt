package com.georgevik.turnia.ui.main.groups.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.domain.repository.GroupRepository
import com.georgevik.turnia.ui.main.groups.model.GroupRowUi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class AdminGroupsViewModel(groupRepository: GroupRepository) : ViewModel() {

    val uiState: StateFlow<AdminGroupsUi> = groupRepository.getGroups()
        .map { groups ->
            AdminGroupsUi.Success(
                groups = groups.filter { it.isAdmin }.map {
                    GroupRowUi(
                        id = it.id,
                        name = it.name,
                        members = it.memberCount,
                        isAdmin = true,
                    )
                }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT),
            initialValue = AdminGroupsUi.Loading,
        )

    private companion object {
        const val SUBSCRIPTION_TIMEOUT = 5_000L
    }
}
