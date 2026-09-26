package com.geoviksoft.turnia.ui.main.group.externalcalendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.PersonalOneOffEvent
import com.geoviksoft.turnia.core.domain.model.PersonalTypedEvent
import com.geoviksoft.turnia.core.domain.model.SharedCalendarError
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.domain.repository.AdRepository
import com.geoviksoft.turnia.core.domain.repository.GroupRepository
import com.geoviksoft.turnia.core.domain.repository.SharedCalendarRepository
import com.geoviksoft.turnia.core.domain.repository.UserRepository
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.navigation.main.routes.ExternalCalendarData
import com.geoviksoft.turnia.ui.components.calendar.model.DayEventUi
import com.geoviksoft.turnia.ui.components.calendar.model.swapFirst
import com.geoviksoft.turnia.ui.components.calendar.model.toUi
import com.geoviksoft.turnia.ui.components.daydetail.model.OneOffEventUi
import com.geoviksoft.turnia.ui.components.daydetail.model.toUiByDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.todayIn
import kotlinx.datetime.yearMonth
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class ExternalCalendarViewModel(
    val data: ExternalCalendarData,
    private val groupRepository: GroupRepository,
    private val sharedCalendarRepository: SharedCalendarRepository,
    private val userRepository: UserRepository,
    adRepository: AdRepository,
) : ViewModel() {
    // A month, not a day: the screen opens on today and the grid then reports the 1st. A StateFlow
    // drops an equal value, so two days of the same month ask once.
    private val month = MutableStateFlow(Clock.System.todayIn(TimeZone.currentSystemDefault()).yearMonth)
    private val invalidateData = MutableStateFlow(1)

    private val _uiState = MutableStateFlow(GroupCalendarUi())
    val uiState: StateFlow<GroupCalendarUi> = _uiState.asStateFlow()

    init {
        // Here and not in the screen: the ViewModel lives once per visit, the composable recomposes.
        adRepository.actionPerformed()

        viewModelScope.launch {
            combine(month, invalidateData) { month, _ -> month }
                .flatMapLatest { month -> events(month) }
                .collect { month ->
                    _uiState.update {
                        it.copy(loading = false, events = month.events, oneOffs = month.oneOffs)
                    }
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
     * Both paint from the device first and again once the server has something newer, so the month
     * shows without waiting on a round trip: a group's through Firestore's own cache, a colleague's
     * through the shared calendar's, which follows their changes for as long as it is on screen.
     */
    private fun events(month: YearMonth): Flow<MonthEvents> =
        userRepository.loggedUserFlow.flatMapLatest { user ->
            val uid = user.id
            val events = when (data) {
                // Revocation is followed rather than read once: a member removed while the
                // calendar is open must lose the swap controls, not keep them until a reload.
                is ExternalCalendarData.Group -> isRevoked().flatMapLatest { revoked ->
                    groupRepository.getEventsByGroup(GroupId(data.id), month.firstDay, monthDelta = 2)
                        .map { list ->
                            val events = list.map {
                                it.toUi(
                                    currentUserId = uid,
                                    removable = it.ownerId == uid && it.assigneeId == uid,
                                    activeMember = !revoked,
                                )
                            }
                            // One-off events are personal: a group's calendar has none.
                            MonthEvents(events.groupBy { it.date }.swapFirst(), emptyMap())
                        }
                }

                is ExternalCalendarData.Personal ->
                    sharedCalendar(UserId(data.id), viewerId = uid, month = month)
            }

            events
        }

    private fun isRevoked(): Flow<Boolean> = groupRepository.getGroups()
        .map { groups -> groups.find { it.id.value == data.id }?.isRevoked == true }
        .distinctUntilChanged()

    private fun sharedCalendar(
        ownerId: UserId,
        viewerId: UserId,
        month: YearMonth,
    ): Flow<MonthEvents> =
        sharedCalendarRepository.sharedCalendar(ownerId, month)
            // Flagged here rather than emitted: an empty emission would wipe the month on screen
            // while the next one is on its way. A month the device has answers in milliseconds.
            .onStart { _uiState.update { it.copy(loading = true) } }
            .map { outcome ->
                when (outcome) {
                    is Outcome.Success -> {
                        val groupEvents = outcome.value.groupEvents.map {
                            it.toUi(currentUserId = viewerId, removable = false)
                        }
                        val personalEvents = outcome.value.personalEvents
                            .filterIsInstance<PersonalTypedEvent>()
                            .map { it.toUi(removable = false) }
                        // The owner's, shown as they are: the sheet offers no way to change them on a
                        // calendar that is not the viewer's, and the rules would refuse the write.
                        val oneOffs = outcome.value.personalEvents
                            .filterIsInstance<PersonalOneOffEvent>()
                            .toUiByDate()
                        MonthEvents(
                            events = (groupEvents + personalEvents).groupBy { it.date }.swapFirst(),
                            oneOffs = oneOffs,
                        )
                    }

                    // Only reached with nothing cached for the month, or once the grant is gone.
                    is Outcome.Failure -> {
                        _uiState.update { it.copy(userMessage = outcome.error) }
                        MonthEvents(emptyMap(), emptyMap())
                    }
                }
            }

    fun onMonthChanged(date: LocalDate) {
        month.update { date.yearMonth }
    }

    fun userMessageShown() = _uiState.update { it.copy(userMessage = null) }
}


private data class MonthEvents(
    val events: Map<LocalDate, List<DayEventUi>>,
    val oneOffs: Map<LocalDate, List<OneOffEventUi>>,
)

data class GroupCalendarUi(
    val events: Map<LocalDate, List<DayEventUi>> = emptyMap(),
    val oneOffs: Map<LocalDate, List<OneOffEventUi>> = emptyMap(),
    val loading: Boolean = true,
    /** The user was removed from this group: the leftover events show, nothing can be added. */
    val isRevoked: Boolean = false,
    val userMessage: SharedCalendarError? = null,
)
