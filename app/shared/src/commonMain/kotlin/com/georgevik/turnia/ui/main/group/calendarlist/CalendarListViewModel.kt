package com.georgevik.turnia.ui.main.group.calendarlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.domain.repository.GroupRepository
import com.georgevik.turnia.core.domain.repository.UserRepository
import com.georgevik.turnia.core.system.valueOrEmpty
import com.georgevik.turnia.ui.main.group.calendarlist.model.ColleageRowUi
import com.georgevik.turnia.ui.main.group.calendarlist.model.GroupRowUi
import com.georgevik.turnia.ui.main.group.calendarlist.model.GroupScreenUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class CalendarListViewModel(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private var allColleagues: List<ColleageRowUi> = emptyList()
    private var allGroups: List<GroupRowUi> = emptyList()

    private val _uiState = MutableStateFlow(
        GroupScreenUi(groups = emptyList(), colleagues = emptyList(), showLoading = true)
    )
    val uiState: StateFlow<GroupScreenUi> = _uiState.asStateFlow()

    private val _searchFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            _searchFlow.debounce(200.milliseconds).collectLatest { q -> performSearch(q) }
        }
        refresh()
    }

    /** Re-reads the groups; a group created on the detail screen only shows up after this. */
    fun refresh() {
        viewModelScope.launch {
            allGroups = groupRepository.getGroups()
                .map { GroupRowUi(it.id, it.name, it.memberCount) }

            allColleagues = userRepository.getCalendarsSharedWithMe().valueOrEmpty()
                .map { profile -> ColleageRowUi(id = profile.id, name = profile.name) }

            performSearch(_searchFlow.value)
        }
    }

    fun searchBy(q: String) {
        viewModelScope.launch { _searchFlow.emit(q) }
    }

    private fun performSearch(q: String) {
        _uiState.update {
            it.copy(
                colleagues = allColleagues.filter { row -> row.name.contains(q, ignoreCase = true) },
                groups = allGroups.filter { row -> row.name.contains(q, ignoreCase = true) },
                showLoading = false,
            )
        }
    }
}
