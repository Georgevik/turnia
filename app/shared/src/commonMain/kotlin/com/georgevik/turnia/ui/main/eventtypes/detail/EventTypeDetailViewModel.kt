package com.georgevik.turnia.ui.main.eventtypes.detail

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.domain.model.EventTypeId
import com.georgevik.turnia.core.domain.model.GroupEventType
import com.georgevik.turnia.core.domain.model.GroupId
import com.georgevik.turnia.core.domain.model.PersonalEventType
import com.georgevik.turnia.core.domain.repository.GroupRepository
import com.georgevik.turnia.core.domain.repository.PersonalEventRepository
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.createId
import com.georgevik.turnia.core.system.fold
import com.georgevik.turnia.core.system.toFailure
import com.georgevik.turnia.core.system.toSuccess
import com.georgevik.turnia.core.system.valueOrNull
import com.georgevik.turnia.navigation.routes.EventTypeDetailData
import com.georgevik.turnia.ui.main.eventtypes.detail.model.EventTypeDetailUi
import com.georgevik.turnia.ui.main.eventtypes.detail.model.EventTypeDetailUi.EventTypeForm
import com.georgevik.turnia.ui.main.eventtypes.detail.model.EventTypeFieldError
import com.georgevik.turnia.ui.main.eventtypes.detail.model.EventTypeScreenError
import com.georgevik.turnia.ui.main.eventtypes.detail.model.EventTypeTitle
import com.georgevik.turnia.ui.main.eventtypes.detail.model.EventTypeToastError
import com.georgevik.turnia.ui.system.EntityPalette
import com.georgevik.turnia.ui.system.components.time.toTimeInput
import com.georgevik.turnia.ui.system.components.time.toTimeOrNull
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
                is EventTypeDetailData.EditGroup -> loadGroupType(GroupId(key.groupId), EventTypeId(key.typeId))
                is EventTypeDetailData.EditPersonal -> loadPersonalType(EventTypeId(key.typeId))
                EventTypeDetailData.NewPersonal,
                is EventTypeDetailData.NewGroup -> newForm().toSuccess()

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
        groupId: GroupId,
        typeId: EventTypeId
    ): Outcome<EventTypeForm, EventTypeScreenError> {
        val group =
            groupRepository.getGroup(groupId).valueOrNull()
                ?: return EventTypeScreenError.GroupNotFound.toFailure()

        return group.types.find { it.id == typeId }?.toUi()?.toSuccess()
            ?: EventTypeScreenError.GroupEventNotFound.toFailure()
    }

    private suspend fun loadPersonalType(typeId: EventTypeId): Outcome<EventTypeForm, EventTypeScreenError> {
        val eventType =
            personalRepository.getMyEventTypes(includeDeleted = true).find { it.id == typeId }
                ?: return EventTypeScreenError.GroupEventNotFound.toFailure()

        return eventType.toUi().toSuccess()
    }

    fun onPickColor(color: Color) {
        viewModelScope.launch {
            when (key) {
                is EventTypeDetailData.EditGroup -> groupRepository.saveTypeColor(
                    typeId = EventTypeId(key.typeId),
                    groupId = GroupId(key.groupId),
                    color = color.toHex()
                )

                is EventTypeDetailData.EditPersonal,
                EventTypeDetailData.NewPersonal,
                // Nothing to colour yet: the pick is saved with the type it belongs to.
                is EventTypeDetailData.NewGroup -> Result.success(Unit)
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

        val errors = state.formErrors
        state.copy(
            form = newForm,
            formErrors = errors.copy(
                nameError = if (field == EventTypeField.Name) null else errors.nameError,
                acronymError = if (field == EventTypeField.Acronym) null else errors.acronymError,
            )
        )
    }

    fun onSave() {
        val state = uiState.value as? EventTypeDetailUi.Success ?: return
        val form = state.form

        val nameError = EventTypeFieldError.Required.takeIf { form.name.isBlank() }
        val acronymError = EventTypeFieldError.Required.takeIf { form.acronym.isBlank() }
        if (nameError != null || acronymError != null) {
            return updateSuccess {
                it.copy(
                    formErrors = it.formErrors.copy(
                        nameError = nameError,
                        acronymError = acronymError
                    )
                )
            }
        }

        when (key) {
            is EventTypeDetailData.EditPersonal -> savePersonal(EventTypeId(key.typeId), form)
            EventTypeDetailData.NewPersonal -> savePersonal(EventTypeId(createId()), form)
            is EventTypeDetailData.NewGroup -> saveGroupType(GroupId(key.groupId), form)
            is EventTypeDetailData.EditGroup ->
                updateSuccess { it.copy(toastError = EventTypeToastError.NotImplemented) }
        }
    }

    private fun savePersonal(typeId: EventTypeId, form: EventTypeForm) {
        val type = PersonalEventType(
            id = typeId,
            name = form.name.trim(),
            color = form.color.toHex(),
            acronym = form.acronym.trim(),
            description = form.description.trim().ifBlank { null },
            startTime = form.startTime.toTimeOrNull(),
            endTime = form.endTime.toTimeOrNull(),
        )

        viewModelScope.launch {
            updateSuccess { it.copy(saveButtonLoading = true) }

            personalRepository.saveEventType(type).fold(
                onSuccess = {
                    updateSuccess {
                        it.copy(
                            saveButtonLoading = false,
                            isSaved = true
                        )
                    }
                },
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

    private fun saveGroupType(groupId: GroupId, form: EventTypeForm) {
        val typeId = EventTypeId(createId())
        val type = GroupEventType(
            id = typeId,
            groupId = groupId,
            // The type does not store the group's name; the mapper drops it on the way out.
            groupName = "",
            name = form.name.trim(),
            acronym = form.acronym.trim(),
            description = form.description.trim().ifBlank { null },
            startTime = form.startTime.toTimeOrNull(),
            endTime = form.endTime.toTimeOrNull(),
            // A type nobody can offer for swap defeats the point of the group; the screen has no
            // switch for it yet.
            swappable = true,
            colorHex = "",
            userColor = null,
        )

        viewModelScope.launch {
            updateSuccess { it.copy(saveButtonLoading = true) }

            groupRepository.saveEventType(groupId, type).fold(
                onSuccess = {
                    // The colour is the author's own pick, not the type's: it is saved apart, and
                    // a group type nobody coloured still renders.
                    groupRepository.saveTypeColor(groupId, typeId, form.color.toHex())
                    updateSuccess { it.copy(saveButtonLoading = false, isSaved = true) }
                },
                onFailure = {
                    updateSuccess {
                        it.copy(
                            toastError = EventTypeToastError.SaveGroup,
                            saveButtonLoading = false,
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
        acronym = acronym,
        description = description.orEmpty(),
        startTime = startTime.orEmpty().toTimeInput(),
        endTime = endTime.orEmpty().toTimeInput(),
        color = color.toComposeColorOr(entityColor(id.value)),
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
        color = color.toComposeColorOr(entityColor(id.value)),
        swappable = false,
    )


    private fun updateSuccess(block: (EventTypeDetailUi.Success) -> EventTypeDetailUi.Success) =
        _uiState.update { if (it is EventTypeDetailUi.Success) block(it) else it }

    companion object {
        private fun newForm() = EventTypeForm(
            typeId = null,
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
