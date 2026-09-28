package com.geoviksoft.turnia.ui.components.movetogroup

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoviksoft.turnia.core.domain.model.EventId
import com.geoviksoft.turnia.core.domain.model.EventTypeId
import com.geoviksoft.turnia.core.domain.model.Group
import com.geoviksoft.turnia.core.domain.model.GroupEventType
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.MoveError
import com.geoviksoft.turnia.core.domain.model.MoveScope
import com.geoviksoft.turnia.core.domain.model.PersonalTypedEvent
import com.geoviksoft.turnia.core.domain.repository.GroupRepository
import com.geoviksoft.turnia.core.domain.repository.PersonalEventRepository
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.ui.components.calendar.model.DayEventUi
import com.geoviksoft.turnia.ui.system.color.entityColor
import com.geoviksoft.turnia.ui.system.color.toComposeColorOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/** The personal shift a move starts from, as its row knows it. */
data class MoveRequest(
    val eventId: EventId,
    val date: LocalDate,
    val typeId: EventTypeId,
    val notes: String?,
) {
    companion object {
        fun of(event: DayEventUi): MoveRequest? = event.typeId?.let { typeId ->
            MoveRequest(event.id, event.date, typeId, event.notes)
        }
    }
}

@Immutable
data class MoveGroupUi(val id: GroupId, val name: String, val color: Color)

@Immutable
data class MoveTypeUi(val id: EventTypeId, val name: String, val acronym: String?, val color: Color)

@Immutable
sealed interface MoveStep {
    data object Loading : MoveStep
    data class PickGroup(val groups: List<MoveGroupUi>) : MoveStep
    data class PickType(val group: MoveGroupUi, val types: List<MoveTypeUi>) : MoveStep

    /** [count] is every event "all" would move, the tapped one included. */
    data class PickScope(val typeName: String, val count: Int) : MoveStep
    data object Moving : MoveStep
    data class Done(val groupName: String, val moved: Int, val skipped: Int) : MoveStep
    data class Failed(val error: MoveError) : MoveStep
}

/**
 * Group, then type, then — only when the type has other events to take along — one or all. Every
 * answer, a failure included, is a step on screen: nothing is sent to the sheet as a one-off event.
 */
class MoveToGroupViewModel(
    private val request: MoveRequest,
    private val groupRepository: GroupRepository,
    private val personalRepository: PersonalEventRepository,
) : ViewModel() {

    private val _step = MutableStateFlow<MoveStep>(MoveStep.Loading)
    val step = _step.asStateFlow()

    private var groups: List<Group> = emptyList()
    private var group: Group? = null
    private var target: GroupEventType? = null
    private var tapped: PersonalTypedEvent? = null
    private var candidates: List<PersonalTypedEvent> = emptyList()

    init {
        viewModelScope.launch {
            val type = personalRepository.getMyEventTypes(includeDeleted = true).first()
                .find { it.id == request.typeId }
            if (type == null) {
                _step.value = MoveStep.Failed(MoveError.Failed)
                return@launch
            }
            tapped = PersonalTypedEvent(request.eventId, type, request.date, request.notes)
            groups = groupRepository.getGroups().first().filter { it.canReceiveMovedEvents }

            when (groups.size) {
                0 -> _step.value = MoveStep.Failed(MoveError.Failed)
                1 -> showTypes(groups.single())
                else -> _step.value = MoveStep.PickGroup(groups.map { it.toUi() })
            }
        }
    }

    fun pickGroup(id: GroupId) {
        if (_step.value !is MoveStep.PickGroup) return
        groups.find { it.id == id }?.let(::showTypes)
    }

    fun pickType(id: EventTypeId) {
        if (_step.value !is MoveStep.PickType) return
        val type = group?.moveTargetTypes?.find { it.id == id } ?: return
        val event = tapped ?: return
        target = type
        _step.value = MoveStep.Loading

        viewModelScope.launch {
            when (val outcome = personalRepository.moveCandidates(event)) {
                is Outcome.Failure -> _step.value = MoveStep.Failed(outcome.error)
                is Outcome.Success -> {
                    // The server's copy of the tapped event, notes included; an event older than
                    // the window is not in the list, and still moves on its own.
                    val found = outcome.value.find { it.id == event.id }
                    if (found != null) tapped = found
                    candidates = if (found != null) outcome.value else outcome.value + event

                    if (candidates.size <= 1) move(MoveScope.One)
                    else _step.value = MoveStep.PickScope(event.type.name, candidates.size)
                }
            }
        }
    }

    fun pickScope(scope: MoveScope) {
        if (_step.value !is MoveStep.PickScope) return
        move(scope)
    }

    private fun move(scope: MoveScope) {
        val type = target ?: return
        val event = tapped ?: return
        val events = if (scope == MoveScope.One) listOf(event) else candidates
        _step.value = MoveStep.Moving

        viewModelScope.launch {
            _step.value = when (val outcome = personalRepository.moveToGroup(events, type, scope)) {
                is Outcome.Success ->
                    MoveStep.Done(type.groupName, outcome.value.moved, outcome.value.skipped)
                is Outcome.Failure -> MoveStep.Failed(outcome.error)
            }
        }
    }

    private fun showTypes(group: Group) {
        this.group = group
        _step.update {
            MoveStep.PickType(
                group = group.toUi(),
                types = group.moveTargetTypes.map { type ->
                    MoveTypeUi(
                        id = type.id,
                        name = type.name,
                        acronym = type.acronym,
                        color = type.color.toComposeColorOrNull() ?: entityColor(type.id.value),
                    )
                },
            )
        }
    }

    private fun Group.toUi() = MoveGroupUi(
        id = id,
        name = name,
        color = color?.toComposeColorOrNull() ?: entityColor(id.value),
    )
}
