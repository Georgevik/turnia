package com.georgevik.turnia.ui.components.daydetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.domain.model.EventType
import com.georgevik.turnia.core.domain.model.Group
import com.georgevik.turnia.core.domain.model.GroupEvent
import com.georgevik.turnia.core.domain.model.GroupEventType
import com.georgevik.turnia.core.domain.model.PersonalEvent
import com.georgevik.turnia.core.domain.model.PersonalEventType
import com.georgevik.turnia.core.domain.repository.GroupRepository
import com.georgevik.turnia.core.domain.repository.PersonalEventRepository
import com.georgevik.turnia.core.domain.repository.UserRepository
import com.georgevik.turnia.core.system.fold
import com.georgevik.turnia.core.system.outcomeCatching
import com.georgevik.turnia.ui.components.calendar.model.CalendarEventUi
import com.georgevik.turnia.ui.components.calendar.model.EventSource
import com.georgevik.turnia.ui.components.daydetail.components.EventTypeChipUi
import com.georgevik.turnia.ui.components.daydetail.model.AddEventTypesError
import com.georgevik.turnia.ui.components.daydetail.model.AddEventTypesUi
import com.georgevik.turnia.ui.components.daydetail.model.EventTypeSectionUi
import com.georgevik.turnia.ui.components.daydetail.model.EventTypeUi
import com.georgevik.turnia.ui.system.entityColor
import com.georgevik.turnia.ui.system.toComposeColorOr
import com.georgevik.turnia.ui.system.toHex
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlin.uuid.Uuid

class DayDetailSheetViewModel(
    private val date: LocalDate,
    private val addMode: DayAddMode,
    private val groupRepository: GroupRepository,
    private val personalRepository: PersonalEventRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddEventTypesUi>(AddEventTypesUi.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        loadEventTypes()
    }

    fun retry() = loadEventTypes()

    private fun loadEventTypes() {
        viewModelScope.launch {
            _uiState.update { AddEventTypesUi.Loading }

            val outcome = outcomeCatching({ AddEventTypesError.LoadFailed }) {
                coroutineScope {
                    val groups = async { groupRepository.getGroups() }
                    val personalTypes = async { personalRepository.getEventTypes() }
                    buildSections(personalTypes.await(), groups.await())
                }
            }

            _uiState.update {
                outcome.fold(
                    onSuccess = { sections -> AddEventTypesUi.Success(sections) },
                    onFailure = { error -> AddEventTypesUi.Error(error) },
                )
            }
        }
    }

    fun removeEvent(event: CalendarEventUi) {
        viewModelScope.launch {
            when (event.source) {
                EventSource.GROUP -> groupRepository.deleteEvent(event.id)
                EventSource.PERSONAL -> personalRepository.deleteEvent(event.id)
            }
        }
    }

    fun addEventOfType(eventTypeUi: EventTypeUi) {
        when (val eventType = eventTypeUi.eventType) {
            is GroupEventType -> addNewEvent(eventType, eventTypeUi)
            is PersonalEventType -> addNewEvent(eventType)
        }
    }

    private fun addNewEvent(type: GroupEventType, eventTypeUi: EventTypeUi) {
        viewModelScope.launch {
            val userId = userRepository.loggedUser?.uid ?: return@launch // TODO Emit error
            groupRepository.addEvent(
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
                    colorHex = eventTypeUi.chipUi.color.toHex(),
                    history = emptyList(),
                )
            )
        }
    }

    private fun addNewEvent(type: PersonalEventType) {
        viewModelScope.launch {
            personalRepository.addEvent(
                PersonalEvent(
                    id = Uuid.random().toString(),
                    type = type,
                    date = date,
                    notes = null,
                )
            )
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
                    source = EventTypeSectionUi.Source.Personal,
                    events = personalTypes.map { it.toUi() },
                )
            )

            else -> emptyList()
        }
        val groupSections = visibleGroups.map { group ->
            EventTypeSectionUi(
                source = EventTypeSectionUi.Source.Group(group.id, group.name),
                events = group.types.map { it.toUi() },
            )
        }

        return (personalSection + groupSections).filter { it.events.isNotEmpty() }
    }

    private fun EventType.toUi() = EventTypeUi(
        chipUi = EventTypeChipUi(
            title = acronym ?: name,
            color = color.toComposeColorOr(entityColor(id))
        ),
        eventType = this,
    )
}
