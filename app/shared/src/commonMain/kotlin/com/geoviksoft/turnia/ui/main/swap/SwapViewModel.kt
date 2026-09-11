package com.geoviksoft.turnia.ui.main.swap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.core.domain.model.Group
import com.geoviksoft.turnia.core.domain.model.GroupEvent
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.SwapError
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.domain.model.UserProfile
import com.geoviksoft.turnia.core.domain.repository.GroupRepository
import com.geoviksoft.turnia.core.domain.repository.UserRepository
import com.geoviksoft.turnia.core.system.onFailure
import com.geoviksoft.turnia.core.system.valueOrNull
import com.geoviksoft.turnia.ui.components.calendar.model.DayEventUi
import com.geoviksoft.turnia.ui.components.calendar.model.toUi
import com.geoviksoft.turnia.ui.main.swap.model.SwapGroupFilterUi
import com.geoviksoft.turnia.ui.main.swap.model.SwapMessage
import com.geoviksoft.turnia.ui.main.swap.model.SwapRequesterUi
import com.geoviksoft.turnia.ui.main.swap.model.SwapRowUi
import com.geoviksoft.turnia.ui.main.swap.model.SwapSegment
import com.geoviksoft.turnia.ui.main.swap.model.SwapUi
import com.geoviksoft.turnia.ui.system.color.entityColor
import com.geoviksoft.turnia.ui.system.color.toComposeColorOr
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

    private val deselectedGroups = MutableStateFlow<Set<GroupId>>(emptySet())
    private val segment = MutableStateFlow(SwapSegment.MINE)
    private val onlyUncovered = MutableStateFlow(false)
    private val userMessage = MutableStateFlow<SwapMessage?>(null)

    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    private val avatars = MutableStateFlow<Map<UserId, UserProfile.AnimalAvatar>>(emptyMap())

    private val swapEvents = userRepository.loggedUserFlow
        .flatMapLatest { user ->
            groupRepository.getSwapEvents(today).map { events -> ViewerEvents(user.id, events) }
        }
        .catch { throwable -> Logger.e(TAG, "Failed to read the swap events", throwable) }
        .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    init {
        viewModelScope.launch {
            combine(
                swapEvents,
                groupRepository.getGroups(),
                avatars,
                combine(deselectedGroups, segment, onlyUncovered, ::Filters),
                userMessage,
            ) { (userId, events), groups, avatars, filters, message ->
                build(userId, events, groups, avatars, filters, message)
            }.collect { state -> _uiState.value = state }
        }

        // Only colleagues' requests show a face, so nothing is read until that list is opened.
        viewModelScope.launch {
            segment.first { it == SwapSegment.COLLEAGUES }
            swapEvents
                .map { (userId, events) ->
                    events.filter { it.requestedByOther(userId) }.map { it.assigneeId }.toSet()
                }
                .distinctUntilChanged()
                .collectLatest(::loadAvatars)
        }
    }

    fun segmentSelected(segment: SwapSegment) = this.segment.update { segment }

    fun groupToggled(groupId: GroupId) = deselectedGroups.update { deselected ->
        if (groupId in deselected) deselected - groupId else deselected + groupId
    }

    fun allGroupsSelected() = deselectedGroups.update { emptySet() }

    fun onlyUncoveredToggled() = onlyUncovered.update { !it }

    fun takeEvent(event: DayEventUi) {
        val groupId = event.groupId ?: return

        viewModelScope.launch {
            groupRepository.takeEvent(groupId, event.id)
                .onFailure { error -> userMessage.update { error.toMessage() } }
        }
    }

    fun userMessageShown() = userMessage.update { null }

    private suspend fun loadAvatars(requesters: Set<UserId>) {
        val missing = requesters - avatars.value.keys
        if (missing.isEmpty()) return

        val profiles = userRepository.getProfiles(missing.toList()).valueOrNull() ?: return
        avatars.update { known -> known + profiles.associate { it.id to it.avatar } }
    }

    private fun build(
        userId: UserId,
        events: List<GroupEvent>,
        groups: List<Group>,
        avatars: Map<UserId, UserProfile.AnimalAvatar>,
        filters: Filters,
        message: SwapMessage?,
    ): SwapUi.Success {
        val revoked = groups.filter { it.isRevoked }.map { it.id }.toSet()
        val visible = events.filter { it.groupId !in filters.deselected }
        val hasCovered = visible.any { it.handedAwayBy(userId) }

        return SwapUi.Success(
            segments = SwapSegment.entries.associateWith { segment ->
                visible.filter { segment.holds(it, userId, revoked) }
                    .filterNot {
                        segment == SwapSegment.MINE && filters.onlyUncovered &&
                            it.handedAwayBy(userId)
                    }
                    .sortedWith(compareBy({ it.date }, { it.id.value }))
                    .map { event -> row(segment, event, userId, revoked, avatars) }
            },
            segment = filters.segment,
            onlyUncovered = filters.onlyUncovered,
            hasCovered = hasCovered,
            groups = groups.map { group ->
                SwapGroupFilterUi(
                    id = group.id,
                    name = group.name,
                    color = group.color.orEmpty().toComposeColorOr(entityColor(group.id.value)),
                    selected = group.id !in filters.deselected,
                )
            },
            userMessage = message,
        )
    }

    /**
     * Each list says one thing about a shift. A shift somebody took from the viewer and now asks to
     * swap again is in both, and each tells its own half: in the viewer's requests, who covers it;
     * among colleagues', who is asking — never both on one card, where they would contradict.
     */
    private fun row(
        segment: SwapSegment,
        event: GroupEvent,
        userId: UserId,
        revoked: Set<GroupId>,
        avatars: Map<UserId, UserProfile.AnimalAvatar>,
    ) = SwapRowUi(
        event = event
            .toUi(currentUserId = userId, activeMember = event.groupId !in revoked),
        requestedBy = if (segment == SwapSegment.COLLEAGUES) {
            SwapRequesterUi(
                name = event.assigneeName,
                avatar = avatars[event.assigneeId] ?: UserProfile.AnimalAvatar.NONE,
            )
        } else {
            null
        },
        coveredBy = event.assigneeName
            .takeIf { segment == SwapSegment.MINE && event.handedAwayBy(userId) },
    )


    private fun SwapSegment.holds(
        event: GroupEvent,
        userId: UserId,
        revoked: Set<GroupId>,
    ): Boolean = when (this) {
        SwapSegment.MINE -> (event.assigneeId == userId && event.onSwap) ||
            event.handedAwayBy(userId)
        // Not from a group the viewer was removed from: they could see the request but never cover it.
        SwapSegment.COLLEAGUES -> event.requestedByOther(userId) && event.groupId !in revoked
    }

    private fun GroupEvent.requestedByOther(userId: UserId) = onSwap && assigneeId != userId

    /**
     * The viewer held this shift once and somebody took it from them. A shift only ever moves by
     * being taken, so every holder in its chain but the last asked to swap it — including one who
     * had taken it from somebody else first. Read off the chain the event already carries: no
     * extra query, and the name comes from the group's own member list.
     */
    private fun GroupEvent.handedAwayBy(userId: UserId) =
        assigneeId != userId && history.any { it.userId == userId }

    private data class ViewerEvents(val userId: UserId, val events: List<GroupEvent>)

    private data class Filters(
        val deselected: Set<GroupId>,
        val segment: SwapSegment,
        val onlyUncovered: Boolean,
    )

    private fun SwapError.toMessage(): SwapMessage = when (this) {
        SwapError.NotMember -> SwapMessage.NotMember
        SwapError.OwnShift -> SwapMessage.OwnShift
        SwapError.NotFound -> SwapMessage.NotFound
        SwapError.TakenBySomeoneElse -> SwapMessage.TakenBySomeoneElse
        SwapError.NotSwappable -> SwapMessage.NotSwappable
        SwapError.NotAssignee, SwapError.SaveFailed -> SwapMessage.SaveFailed
    }

    companion object {
        private const val TAG = "SwapViewModel"
    }
}
