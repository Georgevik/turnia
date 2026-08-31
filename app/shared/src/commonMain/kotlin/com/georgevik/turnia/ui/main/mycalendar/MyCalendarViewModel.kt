package com.georgevik.turnia.ui.main.mycalendar

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.domain.model.GroupEvent
import com.georgevik.turnia.core.domain.model.PersonalEvent
import com.georgevik.turnia.core.domain.model.UserSession
import com.georgevik.turnia.core.domain.repository.GroupRepository
import com.georgevik.turnia.core.domain.repository.PersonalEventRepository
import com.georgevik.turnia.core.domain.repository.UserRepository
import com.georgevik.turnia.ui.components.calendar.model.CalendarEventUi
import com.georgevik.turnia.ui.components.calendar.model.toUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

@Immutable
data class MyCalendarUiState(
    val isLoading: Boolean = false,
    val eventsByDate: Map<LocalDate, List<CalendarEventUi>> = emptyMap(),
)

class MyCalendarViewModel(
    userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val personalRepository: PersonalEventRepository,
) : ViewModel() {

    private val targetDay = MutableStateFlow(Clock.System.todayIn(TimeZone.currentSystemDefault()))
    private val _uiState = MutableStateFlow(MyCalendarUiState())
    val uiState: StateFlow<MyCalendarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                userRepository.userSession.filterIsInstance(UserSession.Authenticated::class),
                targetDay
            ) { userSession, date ->
                _uiState.update { it.copy(isLoading = true) }
                fetchEvents(userSession.user.uid, date)
            }.collectLatest { eventsByDate ->
                _uiState.update { it.copy(isLoading = false, eventsByDate = eventsByDate) }
            }
        }
    }

    fun onMonthChanged(date: LocalDate) {
        targetDay.update { date }
    }

    private suspend fun fetchEvents(
        userId: String,
        date: LocalDate
    ): Map<LocalDate, List<CalendarEventUi>> {
        val personalResult = personalRepository.retrievePersonalEvents(userId, date, monthDelta = 2)
        if (personalResult.isFailure) {
            // TODO Emit error
        }

        val groupResult = groupRepository.retrieveCalendarEvents(userId, date, monthDelta = 2)
        if (groupResult.isFailure) {
            // TODO Emit error
        }

        return mapToUiState(
            groupResult.getOrNull().orEmpty(),
            personalResult.getOrNull().orEmpty()
        )
    }

    private fun mapToUiState(
        groupEvents: List<GroupEvent>, personalEvents: List<PersonalEvent>
    ): Map<LocalDate, List<CalendarEventUi>> {
        val eventsByDate: Map<LocalDate, MutableList<CalendarEventUi>> = buildMap {
            groupEvents.forEach { ev ->
                getOrPut(ev.date) { mutableListOf() }.add(ev.toUi())
            }
            personalEvents.forEach { ev ->
                getOrPut(ev.date) { mutableListOf() }.add(ev.toUi())
            }
        }

        return eventsByDate.mapValues { it.value.toList() }
    }
}
