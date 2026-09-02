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
import com.georgevik.turnia.core.system.fold
import com.georgevik.turnia.core.system.outcomeCatching
import com.georgevik.turnia.ui.components.calendar.model.CalendarEventType
import com.georgevik.turnia.ui.components.calendar.model.CalendarEventUi
import com.georgevik.turnia.ui.components.daydetail.model.AddEventTypesError
import com.georgevik.turnia.ui.components.daydetail.model.AddEventTypesUi
import com.georgevik.turnia.ui.components.daydetail.model.EventTypeSectionUi
import com.georgevik.turnia.ui.components.daydetail.model.PredefinedEventUi
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
                    val personalTypes = async { personalRepository.getPersonalEventTypes() }
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
            val userId = userRepository.user?.uid  ?: return@launch // TODO Emit error
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
        private const val TAG = "DayDetailSheetViewModel"
    }
}
