package com.georgevik.turnia.ui.main.group

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.domain.model.CalendarKind
import com.georgevik.turnia.ui.components.calendar.CalendarEventType
import com.georgevik.turnia.ui.components.calendar.CalendarEventUi
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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

/**
 * Owns the Groups tab state: the lists of colleagues and groups, plus mock event
 * data for the calendar shown when one is opened. Real Firestore data comes later.
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

    fun openCalendar(id: String, kind: CalendarKind) = viewModelScope.launch {
        _uiState.update { it.copy(showLoading = true) }
        _uiState.update {
            it.copy(
                openCalendar = GroupScreenUi.OpenCalendar(
                    id, "UCI Turno Noche", kind, mockEvents()
                )
            )
        }


        _uiState.update { it.copy(showLoading = false) }
        mockEvents()
    }

    fun closeCalendar() {
        _uiState.update { it.copy(openCalendar = null) }
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
        openCalendar = null
    )

    private fun mockEvents(): Map<LocalDate, List<CalendarEventUi>> {
        val teal = Color(0xFF006B5F)
        val slate = Color(0xFF4F6D7A)
        val sand = Color(0xFFFFDDB8)

        fun group(id: String, text: String, color: Color, time: String?, subtitle: String) =
            CalendarEventUi.create(id, CalendarEventType.GROUP, text, color, time, subtitle)

        val firstOfMonth = Clock.System.todayIn(TimeZone.currentSystemDefault())
            .let { LocalDate(it.year, it.month, 1) }

        fun day(offset: Int) = firstOfMonth.plus(offset, DateTimeUnit.DAY)

        return mapOf(
            day(1) to listOf(group(createUuid(), "Turno mañana", slate, "08:00 - 15:00", "Equipo")),
            day(6) to listOf(group(createUuid(), "Guardia noche", teal, "20:00 - 08:00", "Equipo")),
            day(14) to listOf(
                group(createUuid(), "Noche", teal, "22:00 - 06:00", "Equipo"),
                group(createUuid(), "Formación", sand, "16:00 - 18:00", "Sala 3"),
            ),
            day(21) to listOf(group(createUuid(), "Cambio", slate, "07:00 - 15:00", "Equipo")),
        )
    }
}
