package com.georgevik.turnia.ui.main.mycalendar

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.ui.components.calendar.daydetail.model.PredefinedEventUi
import com.georgevik.turnia.ui.components.calendar.model.CalendarEventType
import com.georgevik.turnia.ui.components.calendar.model.CalendarEventUi
import com.georgevik.turnia.ui.system.createUuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

@Immutable
data class MyCalendarUiState(
    val eventsByDate: Map<LocalDate, List<CalendarEventUi>> = emptyMap(),
    val predefinedEvents: List<PredefinedEventUi> = emptyList(),
)

/**
 * Owns the calendar UI state. All render-ready values — including each event's
 * readable text color — are precomputed here so composition only draws.
 */
class MyCalendarViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MyCalendarUiState())
    val uiState: StateFlow<MyCalendarUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = MyCalendarUiState(
            eventsByDate = mockEvents(),
            predefinedEvents = mockPredefined(),
        )
    }

    /** Quick-add: drop a new personal event of the chosen predefined type on [date]. */
    fun addPredefinedEvent(date: LocalDate, predefinedId: String) {
        val predefined = uiState.value.predefinedEvents.firstOrNull { it.id == predefinedId }
            ?: return
        val event = CalendarEventUi.create(
            id = createUuid(),
            type = CalendarEventType.PERSONAL,
            name = predefined.name,
            acronym = predefined.acronym,
            background = predefined.color,
            subtitle = predefined.name,
            isOwner = true,
        )
        _uiState.update { state ->
            val forDay = state.eventsByDate[date].orEmpty() + event
            state.copy(eventsByDate = state.eventsByDate + (date to forDay))
        }
    }

    /** Custom event: will open the "new event" screen. Stubbed until it exists. */
    fun addCustomEvent(date: LocalDate) {
        Logger.d(TAG, "TODO: open new event screen for $date")
    }

    private fun mockPredefined(): List<PredefinedEventUi> {
        val teal = Color(0xFF006B5F)
        val slate = Color(0xFF4F6D7A)
        val amber = Color(0xFFC0873E)
        val sand = Color(0xFFFFDDB8)
        return listOf(
            PredefinedEventUi(createUuid(), "Guardia noche", teal, acronym = "GN"),
            PredefinedEventUi(createUuid(), "Turno mañana", slate, acronym = "M"),
            PredefinedEventUi(createUuid(), "Turno tarde", amber, acronym = "T"),
            PredefinedEventUi(createUuid(), "Gimnasio", sand, acronym = "GYM"),
        )
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
            name: String,
            acronym: String?,
            color: Color,
            time: String?,
            subtitle: String,
            onSwap: Boolean = false,
            isOwner: Boolean = false,
            assignedToOther: Boolean = false,
        ) = CalendarEventUi.create(
            id = id,
            type = CalendarEventType.GROUP,
            name = name,
            acronym = acronym,
            background = color,
            timeRange = time,
            subtitle = subtitle,
            onSwap = onSwap,
            isOwner = isOwner,
            assignedToOther = assignedToOther,
        )

        fun personal(
            id: String,
            name: String,
            acronym: String?,
            color: Color,
            time: String?,
            subtitle: String,
        ) = CalendarEventUi.create(
            id = id,
            type = CalendarEventType.PERSONAL,
            name = name,
            acronym = acronym,
            background = color,
            timeRange = time,
            subtitle = subtitle,
            isOwner = true,
        )

        // Anchor the mock data to the current month so it lands on "today".
        val firstOfMonth = Clock.System.todayIn(TimeZone.currentSystemDefault())
            .let { LocalDate(it.year, it.month, 1) }

        fun day(offset: Int) = firstOfMonth.plus(offset, DateTimeUnit.DAY)

        return mapOf(
            day(0) to listOf(
                group(
                    "g1",
                    "Guardia noche",
                    "GN",
                    teal,
                    "20:00 - 08:00",
                    "Propietario: Yo",
                    onSwap = true,
                    isOwner = true,
                ),
                personal("p1", "Cita médica", null, sand, "09:30 - 10:00", "Revisión anual"),
            ),
            day(3) to listOf(
                group("g2", "Turno mañana", "M", slate, "08:00 - 15:00", "Revisión mensual"),
                group(
                    "g6",
                    "Extra",
                    "EX",
                    teal,
                    "18:00 - 22:00",
                    "La cubre: Marta L.",
                    isOwner = true,
                    assignedToOther = true,
                ),
            ),
            day(9) to listOf(
                group("g3", "Noche", "N", teal, "22:00 - 06:00", "Grupo UCI"),
                personal("p2", "Gimnasio", "GYM", lightAmber, "18:00 - 19:00", "Rutina semanal"),
            ),
            day(15) to listOf(
                group(
                    "g4",
                    "Cambio",
                    "C",
                    red,
                    "07:00 - 15:00",
                    "Grupo UCI",
                    onSwap = true,
                    isOwner = true,
                ),
                personal("p3", "Cena con el equipo", null, sand, "21:00 - 23:00", "Restaurante"),
                group("g5", "Formación", "F", slate, "16:00 - 18:00", "Sala 3"),
                personal("p4", "Recados", null, amber, "12:00 - 13:00", "Varios"),
            ),
            day(22) to listOf(
                personal("p5", "Vacaciones", "VAC", lightAmber, null, "Todo el día"),
            ),
            day(27) to listOf(
                group("g7", "Halloween", null, amber, "20:00 - 23:00", "Fiesta de grupo"),
                personal("p6", "Fiesta", null, teal, "23:00 - 02:00", "Con amigos"),
            ),
        )
    }

    companion object {
        private const val TAG = "MyCalendarViewModel"
    }
}
