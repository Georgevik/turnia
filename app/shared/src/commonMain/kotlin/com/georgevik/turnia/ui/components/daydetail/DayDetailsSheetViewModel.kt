package com.georgevik.turnia.ui.components.daydetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.Group
import com.georgevik.turnia.core.domain.model.GroupEvent
import com.georgevik.turnia.core.domain.model.GroupEventType
import com.georgevik.turnia.core.domain.model.PersonalEvent
import com.georgevik.turnia.core.domain.model.PersonalEventType
import com.georgevik.turnia.core.domain.repository.GroupRepository
import com.georgevik.turnia.core.domain.repository.PersonalEventRepository
import com.georgevik.turnia.core.domain.repository.UserRepository
import com.georgevik.turnia.ui.components.calendar.model.CalendarEventType
import com.georgevik.turnia.ui.components.calendar.model.CalendarEventUi
import com.georgevik.turnia.ui.components.daydetail.model.DayDetailsSheetUiState
import com.georgevik.turnia.ui.components.daydetail.model.EventTypeSectionUi
import com.georgevik.turnia.ui.components.daydetail.model.PredefinedEventUi
import com.georgevik.turnia.ui.system.entityColor
import com.georgevik.turnia.ui.system.toComposeColorOr
import com.georgevik.turnia.ui.system.toHex
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlin.uuid.Uuid

class DayDetailsSheetViewModel(
    private val date: LocalDate,
    private val addMode: DayAddMode,
    private val groupRepository: GroupRepository,
    private val personalRepository: PersonalEventRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    val uiState: StateFlow<DayDetailsSheetUiState> = combine(
        personalRepository.personalEventTypes,
        groupRepository.groups,
    ) { personalTypes, groups ->
        DayDetailsSheetUiState(predefinedSections = buildSections(personalTypes, groups))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DayDetailsSheetUiState(),
    )

    init {
        viewModelScope.launch {
            groupRepository.fetchGroups()
        }
    }

    private val _uiEvent = Channel<DayDetailsSheetUiEvent>()
    val uiEvent: Flow<DayDetailsSheetUiEvent> = _uiEvent.receiveAsFlow()

    fun removeEvent(event: CalendarEventUi) {
        viewModelScope.launch {
            when (event.type) {
                CalendarEventType.GROUP -> groupRepository.deleteGroupEvent(event.id)
                CalendarEventType.PERSONAL -> personalRepository.deletePersonalEvent(event.id)
            }
        }
    }

    fun addPredefinedEvent(predefinedEventUi: PredefinedEventUi) {
        when (val domainObject = predefinedEventUi.domainObject) {
            is GroupEventType -> addNewEvent(domainObject, predefinedEventUi)
            is PersonalEventType -> addNewEvent(domainObject)
            else -> throw IllegalArgumentException("Unknown domain object $domainObject")
        }
    }

    private fun addNewEvent(type: GroupEventType, predefinedEventUi: PredefinedEventUi) {
        viewModelScope.launch {
            val userId = userRepository.userId ?: return@launch // TODO Emit error
            groupRepository.addGroupEvent(
                GroupEvent(
                    id = Uuid.random().toString(),
                    groupId = type.groupId,
                    groupName = type.groupName,
                    ownerId = userId,
                    assigneeId = userId,
                    assigneeName = "",
                    type = type,
                    date = date,
                    onSwap = false,
                    colorHex = predefinedEventUi.color.toHex(),
                    history = emptyList(),
                )
            )
        }
    }

    private fun addNewEvent(type: PersonalEventType) {
        viewModelScope.launch {
            personalRepository.addPersonalEvent(
                PersonalEvent(
                    id = Uuid.random().toString(),
                    type = type,
                    date = date,
                    notes = null,
                )
            )
        }
    }

    fun addCustomEvent() {
        Logger.d(TAG, "TODO: open new event screen for $date")
    }

    fun editGroup(groupId: String, groupName: String) {
        viewModelScope.launch {
            _uiEvent.send(DayDetailsSheetUiEvent.EditGroup(groupId, groupName))
        }
    }

    private fun buildSections(
        personalTypes: List<PersonalEventType>,
        groups: List<Group>,
    ): List<EventTypeSectionUi> {
        val visibleGroups = when (addMode) {
            DayAddMode.Disabled -> return emptyList()
            is DayAddMode.GroupOnly -> groups.filter { it.id == addMode.groupId }
            DayAddMode.Full -> groups
        }
        val personalSection = when (addMode) {
            DayAddMode.Full -> listOf(
                EventTypeSectionUi(
                    type = EventTypeSectionUi.Type.Personal,
                    events = personalTypes.map { it.toPredefined() },
                )
            )

            else -> emptyList()
        }
        val groupSections = visibleGroups.map { group ->
            EventTypeSectionUi(
                type = EventTypeSectionUi.Type.Group(group.id, group.name),
                events = group.types.map { it.toPredefined() },
            )
        }

        return (personalSection + groupSections).filter { it.events.isNotEmpty() }
    }

    private fun PersonalEventType.toPredefined() = PredefinedEventUi(
        id = id,
        title = acronym ?: name,
        color = color.toComposeColorOr(entityColor(id)),
        domainObject = this,
    )

    private fun GroupEventType.toPredefined() = PredefinedEventUi(
        id = id,
        title = acronym ?: name,
        color = color.toComposeColorOr(entityColor(id)),
        domainObject = this
    )

    companion object {
        private const val TAG = "DayDetailsSheetViewModel"
    }
}

sealed interface DayDetailsSheetUiEvent {
    data class EditGroup(val groupId: String, val groupName: String) : DayDetailsSheetUiEvent
}
