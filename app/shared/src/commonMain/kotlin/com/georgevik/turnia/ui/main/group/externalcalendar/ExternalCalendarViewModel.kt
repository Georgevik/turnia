package com.georgevik.turnia.ui.main.group.externalcalendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.domain.model.GroupId
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.domain.repository.GroupRepository
import com.georgevik.turnia.core.domain.repository.PersonalEventRepository
import com.georgevik.turnia.core.domain.repository.UserRepository
import com.georgevik.turnia.core.system.valueOrEmpty
import com.georgevik.turnia.navigation.main.routes.ExternalCalendarData
import com.georgevik.turnia.ui.components.calendar.model.CalendarEventUi
import com.georgevik.turnia.ui.components.calendar.model.toUi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class ExternalCalendarViewModel(
    val data: ExternalCalendarData,
    private val groupRepository: GroupRepository,
    private val personalRepository: PersonalEventRepository,
    private val userRepository: UserRepository,
) : ViewModel() {
    private val monthDate = MutableStateFlow(Clock.System.todayIn(TimeZone.currentSystemDefault()))
    private val invalidateData = MutableStateFlow(1)

    private val _uiState = MutableStateFlow(GroupCalendarUi())
    val uiState: StateFlow<GroupCalendarUi> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(monthDate, invalidateData) { date, _ -> date }
                .flatMapLatest { date -> events(date) }
                .collect { eventsByDate ->
                    _uiState.update { it.copy(loading = false, events = eventsByDate) }
                }
        }

        if (data is ExternalCalendarData.Group) {
            groupRepository.getGroups()
                .onEach { groups ->
                    val group = groups.find { it.id.value == data.id }
                    _uiState.update { it.copy(isRevoked = group?.isRevoked == true) }
                }
                .launchIn(viewModelScope)
        }
    }

    /**
     * Cached events first, then the server's if it had anything newer: the month paints without
     * waiting on a round trip.
     */
    private fun events(date: LocalDate): Flow<Map<LocalDate, List<CalendarEventUi>>> =
        userRepository.loggedUserFlow.flatMapLatest { user ->
            val uid = user.id
            val events = when (data) {
                is ExternalCalendarData.Group ->

                    groupRepository.getEventsByGroup(GroupId(data.id), date, monthDelta = 2)
                        .map { outcome ->
                            outcome.valueOrEmpty().map {
                                it.toUi(
                                    currentUserId = uid,
                                    removable = it.ownerId == uid && it.assigneeId == uid,
                                )
                            }
                        }

                is ExternalCalendarData.Personal ->
                    personalRepository.getEvents(UserId(data.id), date, monthDelta = 2)
                        .map { events -> events.map { event -> event.toUi(removable = false) } }
            }

            events.map { list -> list.groupBy { event -> event.date } }
        }

    fun onMonthChanged(date: LocalDate) {
        monthDate.update { date }
    }
}

data class GroupCalendarUi(
    val events: Map<LocalDate, List<CalendarEventUi>> = emptyMap(),
    val loading: Boolean = true,
    /** The user was removed from this group: the leftover events show, nothing can be added. */
    val isRevoked: Boolean = false,
)
