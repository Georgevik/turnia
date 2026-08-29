package com.georgevik.turnia.ui.main.eventtypes.detail

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.domain.model.GroupEventType
import com.georgevik.turnia.core.domain.model.PersonalEventType
import com.georgevik.turnia.core.domain.repository.GroupRepository
import com.georgevik.turnia.core.domain.repository.PersonalEventRepository
import com.georgevik.turnia.ui.main.eventtypes.detail.model.EventTypeDetailUi
import com.georgevik.turnia.ui.system.createUuid
import com.georgevik.turnia.ui.system.entityColor
import com.georgevik.turnia.ui.system.toComposeColorOr
import com.georgevik.turnia.ui.system.toHex
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.String

class EventTypeDetailViewModel(
    private val typeId: String?,
    val groupId: String?,
    private val groupRepository: GroupRepository,
    private val personalRepository: PersonalEventRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventTypeDetailUi(eventTypeForm = null, loading = true))
    val uiState: StateFlow<EventTypeDetailUi> = _uiState.asStateFlow()

    private val _uiEvent = Channel<EventTypeUiEvent>()
    val uiEvent: Flow<EventTypeUiEvent> = _uiEvent.consumeAsFlow()

    init {
        viewModelScope.launch {
            if (groupId != null && typeId != null) {
                loadGroupType(groupId, typeId)
            } else if (typeId != null) {
                loadPersonalType(typeId)
            } else {
                newForm()
            }
        }
    }

    private suspend fun loadGroupType(groupId: String, typeId: String) {
        val group = groupRepository.getGroup(groupId).getOrNull()

        if (group == null) {
            _uiEvent.send(EventTypeUiEvent.GroupNotFound)
            return
        }
        val eventType = group.types.find { it.id == typeId }
        if (eventType == null) {
            _uiEvent.send(EventTypeUiEvent.GroupEventNotFound)
            return
        }

        val form = eventType.toUi()
        _uiState.update { it.copy(eventTypeForm = form, loading = false) }
    }

    private suspend fun loadPersonalType(typeId: String) {
        val eventType = personalRepository.getEventType(typeId).getOrNull()
        if (eventType == null) {
            _uiEvent.send(EventTypeUiEvent.GroupEventNotFound)
            return
        }

        val form = eventType.toUi()
        _uiState.update { it.copy(eventTypeForm = form, loading = false) }
    }

    fun onPickColor(color: Color) {
        val form = _uiState.value.eventTypeForm?.copy(color = color)

        groupId?.let {
            viewModelScope.launch {
                groupRepository.updateColor(groupId, color.toHex())
            }
        }

        _uiState.update {
            it.copy(eventTypeForm = form)
        }
    }

    fun onFieldChanged(field: EventTypeField, newValue: String) {
        val form = uiState.value.eventTypeForm ?: return
        val newForm = with(form) {
            when (field) {
                EventTypeField.Name -> copy(name = newValue)
                EventTypeField.Acronym -> copy(acronym = newValue)
                EventTypeField.Description -> copy(description = newValue)
                EventTypeField.StartTime -> copy(startTime = newValue)
                EventTypeField.EndTime -> copy(endTime = newValue)
            }
        }

        _uiState.update { it.copy(eventTypeForm = newForm) }
    }

    fun onSavePersonal(): Boolean {
        val form = _uiState.value.eventTypeForm ?: return false
        if (form.name.isBlank()) return false
        val type = PersonalEventType(
            id = typeId ?: "",
            name = form.name.trim(),
            color = form.color.toHex(),
            acronym = form.acronym.trim().ifBlank { null },
            description = form.description.trim().ifBlank { null },
            startTime = form.startTime.trim().ifBlank { null },
            endTime = form.endTime.trim().ifBlank { null },
        )
        personalRepository.update(typeId, type)
        return true
    }

    private fun PersonalEventType.toUi() = EventTypeDetailUi.EventTypeForm(
        typeId = id,
        fieldsEditable = true,
        name = name,
        acronym = acronym.orEmpty(),
        description = description.orEmpty(),
        startTime = startTime.orEmpty(),
        endTime = endTime.orEmpty(),
        color = color.toComposeColorOr(entityColor(id)),
        swappable = false,
    )

    private fun GroupEventType.toUi() = EventTypeDetailUi.EventTypeForm(
        typeId = id,
        fieldsEditable = false,
        name = name,
        acronym = acronym.orEmpty(),
        description = description.orEmpty(),
        startTime = startTime.orEmpty(),
        endTime = endTime.orEmpty(),
        color = color.toComposeColorOr(entityColor(id)),
        swappable = false,
    )

    private fun newForm() = EventTypeDetailUi.EventTypeForm(
        typeId = "",
        fieldsEditable = true,
        name = "",
        acronym = "",
        description = "",
        startTime = "",
        endTime = "",
        color = entityColor(createUuid()),
        swappable = false,
    )

}

enum class EventTypeField { Name, Acronym, Description, StartTime, EndTime }
enum class EventTypeUiEvent { GroupNotFound, GroupEventNotFound }
