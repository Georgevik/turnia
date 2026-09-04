package com.georgevik.turnia.ui.main.group.externalcalendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.domain.model.GroupId
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.domain.repository.GroupRepository
import com.georgevik.turnia.core.domain.repository.PersonalEventRepository
import com.georgevik.turnia.core.domain.repository.UserRepository
import com.georgevik.turnia.core.system.isFailure
import com.georgevik.turnia.core.system.map
import com.georgevik.turnia.core.system.valueOrEmpty
import com.georgevik.turnia.navigation.main.routes.ExternalCalendarData
import com.georgevik.turnia.ui.components.calendar.model.CalendarEventUi
import com.georgevik.turnia.ui.components.calendar.model.toUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class ExternalCalendarViewModel(
    val data: ExternalCalendarData,
    groupRepository: GroupRepository,
    personalRepository: PersonalEventRepository,
    private val userRepository: UserRepository,
) : ViewModel() {
    private val monthDate = MutableStateFlow(Clock.System.todayIn(TimeZone.currentSystemDefault()))
    private val invalidateData = MutableStateFlow(1)

    private val _uiState = MutableStateFlow(GroupCalendarUi())
    val uiState: StateFlow<GroupCalendarUi> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(monthDate, invalidateData) { date, _ -> date }.collect { date ->
                val uid = userRepository.loggedUser?.id ?: return@collect

                val calendarUiEvents = when (data) {
                    is ExternalCalendarData.Group -> groupRepository.getEventsByGroup(
                        GroupId(data.id),
                        date,
                        monthDelta = 2
                    ).map { list ->
                        list.map {
                            it.toUi(
                                currentUserId = uid,
                                removable = it.ownerId == uid && it.assigneeId == uid,
                            )
                        }
                    }

                    is ExternalCalendarData.Personal -> {
                        val result = personalRepository.getEvents(
                            UserId(data.id),
                            date,
                            monthDelta = 2
                        )

                        result.map { events -> events.map { event -> event.toUi(removable = false) } }
                    }
                }

                if (calendarUiEvents.isFailure) {
                    // TODO Emit error
                }

                val eventsByDate = calendarUiEvents.valueOrEmpty().groupBy { event -> event.date }
                _uiState.update { it.copy(loading = false, events = eventsByDate) }
            }

        }
    }

    fun onMonthChanged(date: LocalDate) {
        monthDate.update { date }
    }
}

data class GroupCalendarUi(
    val events: Map<LocalDate, List<CalendarEventUi>> = emptyMap(),
    val loading: Boolean = true,
)
