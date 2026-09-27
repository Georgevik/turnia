package com.geoviksoft.turnia.ui.main.eventtypes.detail

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoviksoft.turnia.core.domain.model.EventTypeId
import com.geoviksoft.turnia.core.domain.model.GroupEventType
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.PersonalEventType
import com.geoviksoft.turnia.core.domain.repository.GroupRepository
import com.geoviksoft.turnia.core.domain.repository.PersonalEventRepository
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.createId
import com.geoviksoft.turnia.core.system.fold
import com.geoviksoft.turnia.core.system.toFailure
import com.geoviksoft.turnia.core.system.toSuccess
import com.geoviksoft.turnia.core.system.valueOrNull
import com.geoviksoft.turnia.navigation.routes.EventTypeDetailData
import com.geoviksoft.turnia.ui.main.eventtypes.detail.model.EventTypeDetailUi
import com.geoviksoft.turnia.ui.main.eventtypes.detail.model.EventTypeDetailUi.EventTypeForm
import com.geoviksoft.turnia.ui.main.eventtypes.detail.model.EventTypeFieldError
import com.geoviksoft.turnia.ui.main.eventtypes.detail.model.EventTypeScreenError
import com.geoviksoft.turnia.ui.main.eventtypes.detail.model.EventTypeTitle
import com.geoviksoft.turnia.ui.main.eventtypes.detail.model.EventTypeToastError
import com.geoviksoft.turnia.ui.system.color.EntityPalette
import com.geoviksoft.turnia.ui.system.color.entityColor
import com.geoviksoft.turnia.ui.system.color.toComposeColorOr
import com.geoviksoft.turnia.ui.system.color.toHex
import com.geoviksoft.turnia.ui.system.components.time.toTimeInput
import com.geoviksoft.turnia.ui.system.components.time.toTimeOrNull
import com.geoviksoft.turnia.ui.system.createUuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

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
                is EventTypeDetailData.EditGroup -> loadGroupType(
                    key.groupId?.let(::GroupId),
                    EventTypeId(key.typeId)
                )

                is EventTypeDetailData.EditPersonal -> loadPersonalType(EventTypeId(key.typeId))
                EventTypeDetailData.NewPersonal -> newForm().toSuccess()
                is EventTypeDetailData.NewGroup -> newForm(isGroupType = true).toSuccess()

            }.fold(
                onSuccess = { form ->
                    _uiState.update {
                        val title = if (form.name.isEmpty()) EventTypeTitle.New
                        else EventTypeTitle.Title(form.name)

                        EventTypeDetailUi.Success(
                            title = title,
                            form = form,
                            colors = EntityPalette,
                        )
                    }
                },
                onFailure = { error -> _uiState.update { EventTypeDetailUi.Error(error) } }
            )
        }
    }


    private suspend fun loadGroupType(
        groupId: GroupId?,
        typeId: EventTypeId
    ): Outcome<EventTypeForm, EventTypeScreenError> {
        if (groupId == null) {
            return groupRepository.pendingEventTypes.value.find { it.id == typeId }
                ?.toUi(editable = true)?.toSuccess()
                ?: EventTypeScreenError.GroupEventNotFound.toFailure()
        }

        val group =
            groupRepository.getGroup(groupId).valueOrNull()
                ?: return EventTypeScreenError.GroupNotFound.toFailure()

        return group.types.find { it.id == typeId }?.toUi(editable = group.isAdmin)?.toSuccess()
            ?: EventTypeScreenError.GroupEventNotFound.toFailure()
    }

    private suspend fun loadPersonalType(typeId: EventTypeId): Outcome<EventTypeForm, EventTypeScreenError> {
        val eventTypes =
            personalRepository.getMyEventTypes(includeDeleted = true).timeout(5.seconds)
                .catch { emit(emptyList()) }
                .firstOrNull()

        val eventType = eventTypes?.find { it.id == typeId }
            ?: return EventTypeScreenError.GroupEventNotFound.toFailure()

        return eventType.toUi().toSuccess()
    }

    fun onPickColor(color: Color) {

        viewModelScope.launch {
            when (key) {
                is EventTypeDetailData.EditGroup -> key.groupId?.let { groupId ->
                    groupRepository.saveTypeColor(
                        typeId = EventTypeId(key.typeId),
                        groupId = GroupId(groupId),
                        color = color.toHex()
                    )
                } ?: Result.success(Unit)

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
                EventTypeField.Acronym -> copy(acronym = newValue.uppercase().take(MAX_ACRONYM_SIZE))
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
            is EventTypeDetailData.EditPersonal -> savePersonal(EventTypeId(key.typeId), form, isNew = false)
            EventTypeDetailData.NewPersonal -> savePersonal(EventTypeId(createId()), form, isNew = true)
            is EventTypeDetailData.NewGroup ->
                saveGroupType(key.groupId?.let(::GroupId), EventTypeId(createId()), form)

            is EventTypeDetailData.EditGroup ->
                saveGroupType(key.groupId?.let(::GroupId), EventTypeId(key.typeId), form)
        }
    }

    private fun savePersonal(typeId: EventTypeId, form: EventTypeForm, isNew: Boolean) {
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

            personalRepository.saveEventType(type, isNew).fold(
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

    private fun saveGroupType(groupId: GroupId?, typeId: EventTypeId, form: EventTypeForm) {
        val type = GroupEventType(
            id = typeId,
            // Never written: a type document carries no group id, the mapper fills this one in
            // from the path on the way back. A draft with no group yet can carry the empty one.
            groupId = groupId ?: GroupId(""),
            groupName = "",
            name = form.name.trim(),
            acronym = form.acronym.trim(),
            description = form.description.trim().ifBlank { null },
            startTime = form.startTime.toTimeOrNull(),
            endTime = form.endTime.toTimeOrNull(),
            swappable = form.swappable ?: true,
            defaultColor = form.color.toHex(),
            userColor = null,
        )

        // Nowhere to write it yet: it waits with the group's other types until the form that
        // is creating the group saves the lot.
        if (groupId == null) {
            groupRepository.setPendingEventType(type)
            updateSuccess { it.copy(isSaved = true) }
            return
        }

        viewModelScope.launch {
            updateSuccess { it.copy(saveButtonLoading = true) }

            groupRepository.saveEventType(groupId, type).fold(
                onSuccess = { updateSuccess { it.copy(saveButtonLoading = false, isSaved = true) } },
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

    private fun GroupEventType.toUi(editable: Boolean) = EventTypeForm(
        typeId = id,
        fieldsEditable = editable,
        name = name,
        acronym = acronym.orEmpty(),
        description = description.orEmpty(),
        startTime = startTime.orEmpty().toTimeInput(),
        endTime = endTime.orEmpty().toTimeInput(),
        color = color.toComposeColorOr(entityColor(id.value)),
        isGroupType = true,
        swappable = swappable,
    )


    private fun updateSuccess(block: (EventTypeDetailUi.Success) -> EventTypeDetailUi.Success) =
        _uiState.update { if (it is EventTypeDetailUi.Success) block(it) else it }

    companion object {
        private fun newForm(isGroupType: Boolean = false) = EventTypeForm(
            typeId = null,
            fieldsEditable = true,
            name = "",
            acronym = "",
            description = "",
            startTime = "",
            endTime = "",
            color = entityColor(createUuid()),
            isGroupType = isGroupType,
            swappable = null,
        )

        const val MAX_ACRONYM_SIZE = 5
    }

}

enum class EventTypeField { Name, Acronym, Description, StartTime, EndTime }
