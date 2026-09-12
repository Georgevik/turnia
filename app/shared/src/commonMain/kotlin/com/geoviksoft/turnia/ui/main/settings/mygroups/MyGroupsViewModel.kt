package com.geoviksoft.turnia.ui.main.settings.mygroups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoviksoft.turnia.core.domain.repository.GroupRepository
import com.geoviksoft.turnia.ui.main.groups.model.GroupRowUi
import com.geoviksoft.turnia.ui.main.groups.model.toRowUi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MyGroupsViewModel(groupRepository: GroupRepository) : ViewModel() {

    val uiState: StateFlow<MyGroupsUi> = groupRepository.getGroups()
        .map { groups -> MyGroupsUi.Success(groups.map { it.toRowUi() }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyGroupsUi.Loading)
}

sealed interface MyGroupsUi {
    data object Loading : MyGroupsUi
    data class Success(val groups: List<GroupRowUi>) : MyGroupsUi
}
