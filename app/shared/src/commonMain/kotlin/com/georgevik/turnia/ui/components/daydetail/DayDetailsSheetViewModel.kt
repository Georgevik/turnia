package com.georgevik.turnia.ui.components.daydetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.Group
import com.georgevik.turnia.core.domain.model.GroupEventType
import com.georgevik.turnia.core.domain.model.PersonalEventType
import com.georgevik.turnia.core.domain.repository.GroupRepository
import com.georgevik.turnia.core.domain.repository.PersonalEventRepository
import com.georgevik.turnia.ui.components.daydetail.model.DayDetailsSheetUiState
import com.georgevik.turnia.ui.components.daydetail.model.PredefinedEventUi
import com.georgevik.turnia.ui.components.daydetail.model.PredefinedSectionUi
import com.georgevik.turnia.ui.system.entityColor
import com.georgevik.turnia.ui.system.toComposeColorOr
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class DayDetailsSheetViewModel(
    private val date: LocalDate,
    groupRepository: GroupRepository,
    personalRepository: PersonalEventRepository,
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

    private val _uiEvent = Channel<DayDetailsSheetUiEvent>()
    val uiEvent: Flow<DayDetailsSheetUiEvent> = _uiEvent.consumeAsFlow()

    fun addPredefinedEvent(predefinedId: String) {
        Logger.d(TAG, "TODO: add predefined $predefinedId on $date")
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
    ): List<PredefinedSectionUi> {
        val personal = PredefinedSectionUi(
            groupId = null,
            groupName = null,
            events = personalTypes.map { it.toPredefined() },
        )
        val groupSections = groups.map { group ->
            PredefinedSectionUi(
                groupId = group.id,
                groupName = group.name,
                events = group.types.map { it.toPredefined() },
            )
        }
        return (listOf(personal) + groupSections).filter { it.events.isNotEmpty() }
    }

    private fun PersonalEventType.toPredefined() = PredefinedEventUi(
        id = id,
        name = name,
        color = color.toComposeColorOr(entityColor(id)),
        acronym = acronym,
    )

    private fun GroupEventType.toPredefined() = PredefinedEventUi(
        id = id,
        name = name,
        color = color.toComposeColorOr(entityColor(id)),
        acronym = acronym,
    )

    companion object {
        private const val TAG = "DayDetailsSheetViewModel"
    }
}

sealed interface DayDetailsSheetUiEvent {
    data class EditGroup(val groupId: String, val groupName: String) : DayDetailsSheetUiEvent
}
