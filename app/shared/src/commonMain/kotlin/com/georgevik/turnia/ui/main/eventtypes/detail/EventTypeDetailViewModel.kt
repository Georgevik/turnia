package com.georgevik.turnia.ui.main.eventtypes.detail

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.domain.model.GroupEventType
import com.georgevik.turnia.core.domain.model.PersonalEventType
import com.georgevik.turnia.core.domain.repository.GroupRepository
import com.georgevik.turnia.core.domain.repository.PersonalEventRepository
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.fold
import com.georgevik.turnia.core.system.mockUuid
import com.georgevik.turnia.core.system.toFailure
import com.georgevik.turnia.core.system.toSuccess
import com.georgevik.turnia.core.system.valueOrNull
import com.georgevik.turnia.navigation.routes.EventTypeDetailData
import com.georgevik.turnia.ui.main.eventtypes.detail.model.EventTypeDetailUi
import com.georgevik.turnia.ui.main.eventtypes.detail.model.EventTypeDetailUi.EventTypeForm
import com.georgevik.turnia.ui.main.eventtypes.detail.model.EventTypeScreenError
import com.georgevik.turnia.ui.main.eventtypes.detail.model.EventTypeTitle
import com.georgevik.turnia.ui.main.eventtypes.detail.model.EventTypeToastError
import com.georgevik.turnia.ui.system.EntityPalette
import com.georgevik.turnia.ui.system.createUuid
import com.georgevik.turnia.ui.system.entityColor
import com.georgevik.turnia.ui.system.toComposeColorOr
import com.georgevik.turnia.ui.system.toHex
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EventTypeDetailViewModel(
    private val key: EventTypeDetailData,
    private val groupRepository: GroupRepository,
    private val personalRepository: PersonalEventRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<EventTypeDetailUi>(EventTypeDetailUi.Loading)
    val uiState: StateFlow<EventTypeDetailUi> = _uiState.asStateFlow()

    init {
        loadForm()
    }

    private fun loadForm() {
        viewModelScope.launch {
            when (key) {
                is EventTypeDetailData.EditGroup -> loadGroupType(key.groupId, key.typeId)
                is EventTypeDetailData.EditPersonal -> loadPersonalType(key.typeId)
                EventTypeDetailData.NewPersonal,
                EventTypeDetailData.NewGroup -> newForm().toSuccess()

            }.fold(
                onSuccess = { form ->
                    _uiState.update {
                        val title = if (form.name.isEmpty()) EventTypeTitle.New
                        else EventTypeTitle.Title(form.name)

                        EventTypeDetailUi.Success(
                            title = title,
                            form = form,
                            colors = EntityPalette
                        )
                    }
                },
                onFailure = { error -> _uiState.update { EventTypeDetailUi.Error(error) } }
            )
        }
    }


    private suspend fun loadGroupType(
        groupId: String,
        typeId: String
    ): Outcome<EventTypeForm, EventTypeScreenError> {
        val group =
            groupRepository.getGroup(groupId).valueOrNull()
                ?: return EventTypeScreenError.GroupNotFound.toFailure()

        return group.types.find { it.id == typeId }?.toUi()?.toSuccess()
            ?: EventTypeScreenError.GroupEventNotFound.toFailure()
    }

    private suspend fun loadPersonalType(typeId: String): Outcome<EventTypeForm, EventTypeScreenError> {
        val eventType = personalRepository.getEventType(typeId).getOrNull()
            ?: return EventTypeScreenError.GroupEventNotFound.toFailure()

        return eventType.toUi().toSuccess()
    }

    fun onPickColor(color: Color) {
        viewModelScope.launch {
            when (key) {
                is EventTypeDetailData.EditGroup -> groupRepository.updateColor(
                    typeId = key.typeId,
                    groupId = key.groupId,
                    color = color.toHex()
                )

                is EventTypeDetailData.EditPersonal,
                EventTypeDetailData.NewPersonal,
                EventTypeDetailData.NewGroup -> Result.success(Unit)
            }.fold(
                onSuccess = { updateSuccess { it.copy(form = it.form.copy(color = color)) } },
                onFailure = { updateSuccess { it.copy(toastError = EventTypeToastError.PickColor) } }
            )
        }
    }

    fun hideMessageError() = updateSuccess { it.copy(toastError = null) }

    fun onFieldChanged(field: EventTypeField, newValue: String) = updateSuccess { state ->
        val newForm = with(state.form) {
            when (field) {
                EventTypeField.Name -> copy(name = newValue)
                EventTypeField.Acronym -> copy(acronym = newValue)
                EventTypeField.Description -> copy(description = newValue)
                EventTypeField.StartTime -> copy(startTime = newValue)
                EventTypeField.EndTime -> copy(endTime = newValue)
            }
        }

        state.copy(form = newForm)
    }

    fun onSavePersonal() {
        val state = uiState.value as? EventTypeDetailUi.Success ?: return
        val typeId = when (key) {
            is EventTypeDetailData.EditPersonal -> key.typeId
            EventTypeDetailData.NewPersonal -> mockUuid()
            is EventTypeDetailData.EditGroup,
            EventTypeDetailData.NewGroup ->
                return updateSuccess { it.copy(toastError = EventTypeToastError.NotImplemented) }
        }

        val form = state.form
        val type = PersonalEventType(
            id = typeId,
            name = form.name.trim(),
            color = form.color.toHex(),
            acronym = form.acronym.trim(),
            description = form.description.trim().ifBlank { null },
            startTime = form.startTime.trim().ifBlank { null },
            endTime = form.endTime.trim().ifBlank { null },
        )

        viewModelScope.launch {
            updateSuccess { it.copy(saveButtonLoading = true) }

            personalRepository.update(type).fold(
                onSuccess = { updateSuccess { it.copy(saveButtonLoading = false, isSaved = true) } },
                onFailure = {
                    updateSuccess {
                        it.copy(
                            toastError = EventTypeToastError.SavePersonal,
                            saveButtonLoading = false
                        )
                    }
                }
            )
        }
    }

    private fun PersonalEventType.toUi() = EventTypeForm(
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

    private fun GroupEventType.toUi() = EventTypeForm(
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


    private fun updateSuccess(block: (EventTypeDetailUi.Success) -> EventTypeDetailUi.Success) =
        _uiState.update { if (it is EventTypeDetailUi.Success) block(it) else it }

    companion object {
        private fun newForm() = EventTypeForm(
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

}

enum class EventTypeField { Name, Acronym, Description, StartTime, EndTime }
