package com.georgevik.turnia.ui.main.mycalendar

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.georgevik.turnia.ui.components.calendar.CalendarEventType
import com.georgevik.turnia.ui.components.calendar.CalendarEventUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

@Immutable
data class MyCalendarUiState(
    val eventsByDate: Map<LocalDate, List<CalendarEventUi>> = emptyMap(),
)

/**
 * Owns the calendar UI state. All render-ready values — including each event's
 * readable text color — are precomputed here so composition only draws.
 */
class MyCalendarViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MyCalendarUiState())
    val uiState: StateFlow<MyCalendarUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = MyCalendarUiState(eventsByDate = mockEvents())
    }

    private fun mockEvents(): Map<LocalDate, List<CalendarEventUi>> {
        // Palette samples that exercise both light- and dark-on-color text.
        val teal = Color(0xFF006B5F)
        val slate = Color(0xFF4F6D7A)
        val amber = Color(0xFFC0873E)
        val lightAmber = Color(0xFFFBBA6C)
        val sand = Color(0xFFFFDDB8)
        val red = Color(0xFFBA1A1A)

        fun group(
            id: String,
            text: String,
            color: Color,
            time: String?,
            subtitle: String,
            onSale: Boolean = false,
            isOwner: Boolean = false,
        ) = CalendarEventUi.create(
            id, CalendarEventType.GROUP, text, color, time, subtitle, onSale, isOwner,
        )

        fun personal(
            id: String,
            text: String,
            color: Color,
            time: String?,
            subtitle: String,
        ) = CalendarEventUi.create(
            id, CalendarEventType.PERSONAL, text, color, time, subtitle, isOwner = true,
        )

        // Anchor the mock data to the current month so it lands on "today".
        val firstOfMonth = Clock.System.todayIn(TimeZone.currentSystemDefault())
            .let { LocalDate(it.year, it.month, 1) }
        fun day(offset: Int) = firstOfMonth.plus(offset, DateTimeUnit.DAY)

        return mapOf(
            day(0) to listOf(
                group("g1", "Guardia noche", teal, "20:00 - 08:00", "Propietario: Yo", onSale = true, isOwner = true),
                personal("p1", "Cita médica", sand, "09:30 - 10:00", "Revisión anual"),
            ),
            day(3) to listOf(
                group("g2", "Turno mañana", slate, "08:00 - 15:00", "Revisión mensual"),
            ),
            day(9) to listOf(
                group("g3", "Noche", teal, "22:00 - 06:00", "Grupo UCI"),
                personal("p2", "Gimnasio", lightAmber, "18:00 - 19:00", "Rutina semanal"),
            ),
            day(15) to listOf(
                group("g4", "Cambio", red, "07:00 - 15:00", "Grupo UCI", onSale = true, isOwner = true),
                personal("p3", "Cena con el equipo", sand, "21:00 - 23:00", "Restaurante"),
                group("g5", "Formación", slate, "16:00 - 18:00", "Sala 3"),
                personal("p4", "Recados", amber, "12:00 - 13:00", "Varios"),
                group("g6", "Extra", teal, "18:00 - 22:00", "Grupo Urgencias"),
            ),
            day(22) to listOf(
                personal("p5", "Vacaciones", lightAmber, null, "Todo el día"),
            ),
            day(27) to listOf(
                group("g7", "Halloween", amber, "20:00 - 23:00", "Fiesta de grupo"),
                personal("p6", "Fiesta", teal, "23:00 - 02:00", "Con amigos"),
            ),
        )
    }
}
