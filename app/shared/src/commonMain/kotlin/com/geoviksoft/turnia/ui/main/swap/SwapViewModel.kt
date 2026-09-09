package com.geoviksoft.turnia.ui.main.swap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoviksoft.turnia.core.domain.model.Group
import com.geoviksoft.turnia.core.domain.model.GroupEvent
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.SwapError
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.domain.repository.GroupRepository
import com.geoviksoft.turnia.core.domain.repository.UserRepository
import com.geoviksoft.turnia.core.system.onFailure
import com.geoviksoft.turnia.ui.components.calendar.model.CalendarEventUi
import com.geoviksoft.turnia.ui.components.calendar.model.toUi
import com.geoviksoft.turnia.ui.main.swap.model.SwapGroupFilterUi
import com.geoviksoft.turnia.ui.main.swap.model.SwapMessage
import com.geoviksoft.turnia.ui.main.swap.model.SwapSegment
import com.geoviksoft.turnia.ui.main.swap.model.SwapUi
import com.geoviksoft.turnia.ui.system.color.entityColor
import com.geoviksoft.turnia.ui.system.color.toComposeColorOr
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class SwapViewModel(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SwapUi>(SwapUi.Loading)
    val uiState = _uiState.asStateFlow()

    // What the user has chosen, kept apart from what the repositories say. Holding these as their
    // own flows is what lets the state be rebuilt as a pure function of its inputs: a new emission
    // of the events cannot forget which tab is open or swallow a message not yet shown.
    /** The groups the user has unticked. Empty means they have not touched the filter. */
    private val deselectedGroups = MutableStateFlow<Set<GroupId>>(emptySet())
    private val segment = MutableStateFlow(SwapSegment.OFFERED)
    private val userMessage = MutableStateFlow<SwapMessage?>(null)

    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    init {
        viewModelScope.launch {
            userRepository.loggedUserFlow.flatMapLatest { user ->
                combine(
                    groupRepository.getSwapEvents(today),
                    groupRepository.getGroups(),
                    deselectedGroups,
                    segment,
                    userMessage,
                ) { events, groups, deselected, segment, message ->
                    build(user.id, events, groups, deselected, segment, message)
                }
            }.collect { state -> _uiState.value = state }
        }
    }

    fun segmentSelected(segment: SwapSegment) = this.segment.update { segment }

    fun groupToggled(groupId: GroupId) = deselectedGroups.update { deselected ->
        if (groupId in deselected) deselected - groupId else deselected + groupId
    }

    fun allGroupsSelected() = deselectedGroups.update { emptySet() }

    fun takeEvent(event: CalendarEventUi) {
        val groupId = event.groupId ?: return

        viewModelScope.launch {
            groupRepository.takeEvent(groupId, event.id)
                .onFailure { error -> userMessage.update { error.toMessage() } }
        }
    }

    fun userMessageShown() = userMessage.update { null }

    private fun build(
        userId: UserId,
        events: List<GroupEvent>,
        groups: List<Group>,
        deselected: Set<GroupId>,
        segment: SwapSegment,
        message: SwapMessage?,
    ): SwapUi.Success {
        val revoked = groups.filter { it.isRevoked }.map { it.id }.toSet()
        val visible = events.filter { it.groupId !in deselected }

        return SwapUi.Success(
            segments = SwapSegment.entries.associateWith { segment ->
                visible.filter { segment.holds(it, userId) }
                    .sortedWith(compareBy({ it.date }, { it.id.value }))
                    .map { it.toUi(currentUserId = userId, activeMember = it.groupId !in revoked) }
            },
            segment = segment,
            groups = groups.map { group ->
                SwapGroupFilterUi(
                    id = group.id,
                    name = group.name,
                    color = group.color.orEmpty().toComposeColorOr(entityColor(group.id.value)),
                    selected = group.id !in deselected,
                )
            },
            userMessage = message,
        )
    }

    /**
     * The four segments, as predicates over who owns a shift and who covers it.
     *
     * `ownerId` is whoever created it and never changes; `assigneeId` is whoever covers it now. So
     * the two of them disagreeing is exactly the record that a transfer happened, and which side of
     * it the user is on says whether they gave the shift away or picked it up.
     */
    private fun SwapSegment.holds(event: GroupEvent, userId: UserId): Boolean = when (this) {
        SwapSegment.OFFERED -> event.assigneeId == userId && event.onSwap
        SwapSegment.AVAILABLE -> event.onSwap && event.assigneeId != userId
        SwapSegment.COVERED -> event.ownerId == userId && event.assigneeId != userId
        SwapSegment.COVERING -> event.assigneeId == userId && event.ownerId != userId
    }

    private fun SwapError.toMessage(): SwapMessage = when (this) {
        SwapError.NotMember -> SwapMessage.NotMember
        SwapError.OwnShift -> SwapMessage.OwnShift
        SwapError.NotFound -> SwapMessage.NotFound
        SwapError.TakenBySomeoneElse -> SwapMessage.TakenBySomeoneElse
        SwapError.NotSwappable -> SwapMessage.NotSwappable
        SwapError.NotAssignee, SwapError.SaveFailed -> SwapMessage.SaveFailed
    }
}
