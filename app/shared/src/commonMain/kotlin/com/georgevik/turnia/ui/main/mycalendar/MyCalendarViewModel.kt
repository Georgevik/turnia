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
import com.georgevik.turnia.core.system.valueOrNull
import com.georgevik.turnia.ui.components.calendar.model.CalendarEventUi
import com.georgevik.turnia.ui.components.calendar.model.toUi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
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

@OptIn(ExperimentalCoroutinesApi::class)
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
            ) { _, _, userSession, date -> userSession.user.id to date }
                .flatMapLatest { (userId, date) -> events(userId, date) }
                .collect { eventsByDate ->
                    _uiState.update { it.copy(isLoading = false, eventsByDate = eventsByDate) }
                }
        }
    }

    fun onMonthChanged(date: LocalDate) {
        targetDay.update { date }
    }

    /**
     * Both sources answer from cache first and again once the server has something newer, so the
     * month paints on the first pair of emissions instead of waiting on the slower of the two.
     */
    private fun events(
        userId: UserId,
        date: LocalDate,
    ): Flow<Map<LocalDate, List<CalendarEventUi>>> = combine(
        personalRepository.getEvents(userId, date, monthDelta = 2),
        groupRepository.getEventsByUser(userId, date, monthDelta = 2),
    ) { personal, group ->
        mapToUiState(userId, group, personal.valueOrNull().orEmpty())
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
