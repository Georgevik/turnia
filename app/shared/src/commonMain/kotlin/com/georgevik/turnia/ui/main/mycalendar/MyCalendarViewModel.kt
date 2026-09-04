package com.georgevik.turnia.ui.main.mycalendar

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.domain.model.GroupEvent
import com.georgevik.turnia.core.domain.model.PersonalEvent
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.domain.model.UserSession
import com.georgevik.turnia.core.domain.repository.GroupRepository
import com.georgevik.turnia.core.domain.repository.PersonalEventRepository
import com.georgevik.turnia.core.domain.repository.UserRepository
import com.georgevik.turnia.core.system.isFailure
import com.georgevik.turnia.core.system.valueOrNull
import com.georgevik.turnia.ui.components.calendar.model.CalendarEventUi
import com.georgevik.turnia.ui.components.calendar.model.toUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.onStart
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
                personalRepository.onEventsChanged.onStart { emit(1) },
                personalRepository.onEventTypeChanged.onStart { emit(1) },
                userRepository.userSession.filterIsInstance(UserSession.Authenticated::class),
                targetDay
            ) { _, _, userSession, date ->
                _uiState.update { it.copy(isLoading = true) }
                fetchEvents(userSession.user.id, date)
            }.collect { eventsByDate ->
                _uiState.update { it.copy(isLoading = false, eventsByDate = eventsByDate) }
            }
        }
    }

    fun onMonthChanged(date: LocalDate) {
        targetDay.update { date }
    }

    private suspend fun fetchEvents(
        uid: UserId,
        date: LocalDate
    ): Map<LocalDate, List<CalendarEventUi>> {
        val personalResult = personalRepository.getEvents(uid, date, monthDelta = 2)
        if (personalResult.isFailure) {
            // TODO Emit error
        }

        val groupResult = groupRepository.getEventsByUser(uid, date, monthDelta = 2)
        if (groupResult.isFailure) {
            // TODO Emit error
        }

        return mapToUiState(
            uid,
            groupResult.getOrNull().orEmpty(),
            personalResult.valueOrNull().orEmpty()
        )
    }

    private fun mapToUiState(
        userId: UserId,
        groupEvents: List<GroupEvent>, personalEvents: List<PersonalEvent>
    ): Map<LocalDate, List<CalendarEventUi>> {
        val eventsByDate: Map<LocalDate, MutableList<CalendarEventUi>> = buildMap {
            groupEvents.forEach { ev ->
                val removable = ev.ownerId == userId && ev.assigneeId == userId
                getOrPut(ev.date) { mutableListOf() }.add(
                    ev.toUi(currentUserId = userId, removable = removable)
                )
            }
            personalEvents.forEach { ev ->
                getOrPut(ev.localDate) { mutableListOf() }.add(ev.toUi(removable = true))
            }
        }

        return eventsByDate.mapValues { it.value.toList() }
    }
}
