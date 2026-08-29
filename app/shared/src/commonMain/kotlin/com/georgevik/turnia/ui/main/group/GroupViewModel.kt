package com.georgevik.turnia.ui.main.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.ui.main.group.model.ColleageRowUi
import com.georgevik.turnia.ui.main.group.model.GroupRowUi
import com.georgevik.turnia.ui.main.group.model.GroupScreenUi
import com.georgevik.turnia.ui.system.createUuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Owns the Groups tab state: the lists of colleagues and groups. Real Firestore data comes
 * later. Opening a group/colleague's calendar is a real navigation entry
 * (`MainRoute.GroupCalendarKey`, backed by [GroupCalendarViewModel]), not state on this view model.
 */
class GroupViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(mockScreen())
    val uiState: StateFlow<GroupScreenUi> = _uiState.asStateFlow()

    private val _searchFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            _searchFlow.debounce(200.milliseconds).collectLatest { q -> performSearch(q) }
        }
    }

    fun searchBy(q: String) {
        viewModelScope.launch { _searchFlow.emit(q) }
    }

    private fun performSearch(q: String) {
        val currentState = uiState.value
        val colleagues = currentState.colleagues.filter { it.name.contains(q, ignoreCase = true) }
        val groups = currentState.groups.filter { it.name.contains(q, ignoreCase = true) }

        _uiState.update { it.copy(colleagues = colleagues, groups = groups) }
    }

    private fun mockScreen(): GroupScreenUi = GroupScreenUi(
        colleagues = listOf(
            ColleageRowUi(createUuid(), "Sarah J.", "RN, Urgencias"),
            ColleageRowUi(createUuid(), "Dr. Chen", "Adjunto, UCI"),
            ColleageRowUi(createUuid(), "Marta L.", "TCAE, Planta 3"),
        ),
        groups = listOf(
            GroupRowUi(createUuid(), "UCI Turno Noche", 12),
            GroupRowUi(createUuid(), "Urgencias", 45),
            GroupRowUi(createUuid(), "Planta 3", 20),
        ),
        showLoading = false,
    )
}
