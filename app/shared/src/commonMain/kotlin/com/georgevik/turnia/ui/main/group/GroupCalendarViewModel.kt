package com.georgevik.turnia.ui.main.group

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.domain.model.CalendarKind
import com.georgevik.turnia.ui.components.calendar.model.CalendarEventType
import com.georgevik.turnia.ui.components.calendar.model.CalendarEventUi
import com.georgevik.turnia.ui.system.createUuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class GroupCalendarViewModel(
    val id: String,
    val name: String,
    val kind: CalendarKind,
) : ViewModel() {
    private val _uiState = MutableStateFlow(GroupCalendarUi())
    val uiState: StateFlow<GroupCalendarUi> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(events = mockEvents(), loading = false) }
        }
    }

    private fun mockEvents(): Map<LocalDate, List<CalendarEventUi>> {
        val teal = Color(0xFF006B5F)
        val slate = Color(0xFF4F6D7A)
        val sand = Color(0xFFFFDDB8)

        fun group(
            id: String,
            name: String,
            acronym: String?,
            color: Color,
            time: String?,
            subtitle: String,
        ) = CalendarEventUi.create(
            id = id,
            type = CalendarEventType.GROUP,
            name = name,
            acronym = acronym,
            background = color,
            timeRange = time,
            subtitle = subtitle,
        )

        val firstOfMonth = Clock.System.todayIn(TimeZone.currentSystemDefault())
            .let { LocalDate(it.year, it.month, 1) }

        fun day(offset: Int) = firstOfMonth.plus(offset, DateTimeUnit.DAY)

        return mapOf(
            day(1) to listOf(group(createUuid(), "Turno mañana", "M", slate, "08:00 - 15:00", "Equipo")),
            day(6) to listOf(group(createUuid(), "Guardia noche", "GN", teal, "20:00 - 08:00", "Equipo")),
            day(14) to listOf(
                group(createUuid(), "Noche", "N", teal, "22:00 - 06:00", "Equipo"),
                group(createUuid(), "Formación", "F", sand, "16:00 - 18:00", "Sala 3"),
            ),
            day(21) to listOf(group(createUuid(), "Cambio", "C", slate, "07:00 - 15:00", "Equipo")),
        )
    }
}

data class GroupCalendarUi(
    val events: Map<LocalDate, List<CalendarEventUi>> = emptyMap(),
    val loading: Boolean = true,
)
