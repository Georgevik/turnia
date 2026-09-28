package com.geoviksoft.turnia.ui.main.mycalendar

import kotlinx.coroutines.flow.StateFlow
import com.geoviksoft.turnia.core.domain.repository.ShiftSetupRepository
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoviksoft.turnia.core.domain.model.EventId
import com.geoviksoft.turnia.core.domain.model.GroupEvent
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.PersonalTypedEvent
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.domain.repository.GroupRepository
import com.geoviksoft.turnia.core.domain.repository.PersonalEventRepository
import com.geoviksoft.turnia.core.domain.repository.UserRepository
import com.geoviksoft.turnia.ui.components.calendar.model.DayEventUi
import com.geoviksoft.turnia.ui.components.calendar.model.swapFirst
import com.geoviksoft.turnia.ui.components.calendar.model.toUi
import com.geoviksoft.turnia.ui.components.daydetail.model.OneOffEventUi
import com.geoviksoft.turnia.ui.components.daydetail.model.toUiByDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

@Immutable
sealed interface MyCalendarUiState {
    object Loading : MyCalendarUiState
    data class Success(
        val eventsByDate: Map<LocalDate, List<DayEventUi>>,
        val oneOffsByDate: Map<LocalDate, List<OneOffEventUi>> = emptyMap(),
    ) : MyCalendarUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class MyCalendarViewModel(
    userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val personalRepository: PersonalEventRepository,
    private val shiftSetupRepository: ShiftSetupRepository,
) : ViewModel() {

    /** "Tap a day": owed once the setup has created the user's shifts, until it has been shown. */
    val shiftHintPending: StateFlow<Boolean> = shiftSetupRepository.hintPending

    fun shiftHintShown() = shiftSetupRepository.hintShown()

    private val targetDay = MutableStateFlow(Clock.System.todayIn(TimeZone.currentSystemDefault()))
    private val _uiState = MutableStateFlow<MyCalendarUiState>(MyCalendarUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(userRepository.loggedUserFlow, targetDay) { user, date -> user.id to date }
                .flatMapLatest { (userId, date) ->
                    combine(
                        events(userId, date),
                        personalRepository.getOneOffEvents(userId, date, monthDelta = 2)
                            .onStart { emit(emptyList()) },
                    ) { events, oneOffs -> events to oneOffs.toUiByDate() }
                }
                .collect { (events, oneOffs) ->
                    _uiState.update {
                        MyCalendarUiState.Success(eventsByDate = events, oneOffsByDate = oneOffs)
                    }
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
        // Most months have none, so the shifts paint without waiting on it.
        groupRepository.getMyEventNotes(date, monthDelta = 2).onStart { emit(emptyMap()) },
    ) { personal, group, revokedGroups, notes ->
        mapToUiState(userId, group, personal, revokedGroups, notes)
    }

    private fun mapToUiState(
        userId: UserId,
        groupEvents: List<GroupEvent>,
        personalEvents: List<PersonalTypedEvent>,
        revokedGroups: Set<GroupId>,
        notes: Map<EventId, String>,
    ): Map<LocalDate, List<DayEventUi>> {
        val eventsByDate: Map<LocalDate, MutableList<DayEventUi>> = buildMap {
            groupEvents.forEach { ev ->
                val removable = ev.ownerId == userId && ev.assigneeId == userId
                getOrPut(ev.date) { mutableListOf() }.add(
                    ev.toUi(
                        currentUserId = userId,
                        removable = removable,
                        activeMember = ev.groupId !in revokedGroups,
                        notes = notes[ev.id],
                        notesEditable = true,
                    )
                )
            }
            personalEvents.forEach { ev ->
                getOrPut(ev.date) { mutableListOf() }.add(
                    ev.toUi(removable = true, notesEditable = true)
                )
            }
        }

        return eventsByDate.mapValues { it.value.toList() }.swapFirst()
    }
}
