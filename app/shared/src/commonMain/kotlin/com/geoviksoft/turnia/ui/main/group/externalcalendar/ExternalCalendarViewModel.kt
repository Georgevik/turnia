package com.geoviksoft.turnia.ui.main.group.externalcalendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.SharedCalendarError
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.domain.repository.GroupRepository
import com.geoviksoft.turnia.core.domain.repository.SharedCalendarRepository
import com.geoviksoft.turnia.core.domain.repository.UserRepository
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.navigation.main.routes.ExternalCalendarData
import com.geoviksoft.turnia.ui.components.calendar.model.DayEventUi
import com.geoviksoft.turnia.ui.components.calendar.model.swapFirst
import com.geoviksoft.turnia.ui.components.calendar.model.toUi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class ExternalCalendarViewModel(
    val data: ExternalCalendarData,
    private val groupRepository: GroupRepository,
    private val sharedCalendarRepository: SharedCalendarRepository,
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
     * A group's events come from the cache first and again once the server has something newer, so
     * the month paints without waiting on a round trip. A colleague's cannot: see [sharedCalendar].
     */
    private fun events(date: LocalDate): Flow<Map<LocalDate, List<DayEventUi>>> =
        userRepository.loggedUserFlow.flatMapLatest { user ->
            val uid = user.id
            val events = when (data) {
                // Revocation is followed rather than read once: a member removed while the
                // calendar is open must lose the swap controls, not keep them until a reload.
                is ExternalCalendarData.Group -> isRevoked().flatMapLatest { revoked ->
                    groupRepository.getEventsByGroup(GroupId(data.id), date, monthDelta = 2)
                        .map { list ->
                            list.map {
                                it.toUi(
                                    currentUserId = uid,
                                    removable = it.ownerId == uid && it.assigneeId == uid,
                                    activeMember = !revoked,
                                )
                            }
                        }
                }

                is ExternalCalendarData.Personal ->
                    sharedCalendar(UserId(data.id), viewerId = uid, date = date)
            }

            events.map { list -> list.groupBy { event -> event.date }.swapFirst() }

        }

    private fun isRevoked(): Flow<Boolean> = groupRepository.getGroups()
        .map { groups -> groups.find { it.id.value == data.id }?.isRevoked == true }
        .distinctUntilChanged()

    private fun sharedCalendar(
        ownerId: UserId,
        viewerId: UserId,
        date: LocalDate,
    ): Flow<List<DayEventUi>> =
        flow {
            // Flagged here rather than emitted: an empty emission would wipe the month on screen
            // while the next one is on its way, and one callable answers for both.
            _uiState.update { it.copy(loading = true) }
            val outcome = sharedCalendarRepository.getSharedCalendar(
                ownerId = ownerId,
                from = date.minus(SHARED_MONTH_DELTA, DateTimeUnit.MONTH),
                to = date.plus(SHARED_MONTH_DELTA, DateTimeUnit.MONTH),
            )

            when (outcome) {
                is Outcome.Success -> {
                    val groupEvents = outcome.value.groupEvents.map {
                        it.toUi(currentUserId = viewerId, removable = false)
                    }
                    val personalEvents =
                        outcome.value.personalEvents.map { it.toUi(removable = false) }
                    emit(groupEvents + personalEvents)
                }

                is Outcome.Failure -> {
                    _uiState.update { it.copy(userMessage = outcome.error) }
                    emit(emptyList())
                }
            }
        }

    fun onMonthChanged(date: LocalDate) {
        monthDate.update { date }
    }

    fun userMessageShown() = _uiState.update { it.copy(userMessage = null) }

    private companion object {
        const val SHARED_MONTH_DELTA = 1
    }
}


data class GroupCalendarUi(
    val events: Map<LocalDate, List<DayEventUi>> = emptyMap(),
    val loading: Boolean = true,
    /** The user was removed from this group: the leftover events show, nothing can be added. */
    val isRevoked: Boolean = false,
    val userMessage: SharedCalendarError? = null,
)
