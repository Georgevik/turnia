package com.geoviksoft.turnia.ui.main.mycalendar

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoviksoft.turnia.core.domain.model.GroupEvent
import com.geoviksoft.turnia.core.domain.model.PersonalEvent
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.domain.repository.GroupRepository
import com.geoviksoft.turnia.core.domain.repository.PersonalEventRepository
import com.geoviksoft.turnia.core.domain.repository.UserRepository
import com.geoviksoft.turnia.ui.components.calendar.model.DayEventUi
import com.geoviksoft.turnia.ui.components.calendar.model.swapFirst
import com.geoviksoft.turnia.ui.components.calendar.model.toUi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.geoviksoft.turnia.core.domain.model.GroupId
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

@Immutable
data class MyCalendarUiState(
    val isLoading: Boolean = false,
    val eventsByDate: Map<LocalDate, List<DayEventUi>> = emptyMap(),
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
            combine(userRepository.loggedUserFlow, targetDay) { user, date -> user.id to date }
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
    ): Flow<Map<LocalDate, List<DayEventUi>>> = combine(
        personalRepository.getEvents(userId, date, monthDelta = 2),
        groupRepository.getEventsByUser(userId, date, monthDelta = 2),
        // The groups the user was removed from. Their leftover shifts still belong on the calendar,
        // but nothing there can be offered or taken any more and the rules refuse the write, so the
        // controls have to know.
        groupRepository.getGroups()
            .map { groups -> groups.filter { it.isRevoked }.map { it.id }.toSet() },
    ) { personal, group, revokedGroups ->
        mapToUiState(userId, group, personal, revokedGroups)
    }

    private fun mapToUiState(
        userId: UserId,
        groupEvents: List<GroupEvent>,
        personalEvents: List<PersonalEvent>,
        revokedGroups: Set<GroupId>,
    ): Map<LocalDate, List<DayEventUi>> {
        val eventsByDate: Map<LocalDate, MutableList<DayEventUi>> = buildMap {
            groupEvents.forEach { ev ->
                val removable = ev.ownerId == userId && ev.assigneeId == userId
                getOrPut(ev.date) { mutableListOf() }.add(
                    ev.toUi(
                        currentUserId = userId,
                        removable = removable,
                        activeMember = ev.groupId !in revokedGroups,
                    )
                )
            }
            personalEvents.forEach { ev ->
                getOrPut(ev.localDate) { mutableListOf() }.add(
                    ev.toUi(removable = true, notesEditable = true)
                )
            }
        }

        return eventsByDate.mapValues { it.value.toList() }.swapFirst()
    }
}
