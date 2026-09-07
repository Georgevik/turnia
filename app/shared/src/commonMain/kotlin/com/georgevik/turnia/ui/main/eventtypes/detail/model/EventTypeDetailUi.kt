package com.georgevik.turnia.ui.main.eventtypes.detail.model

import androidx.compose.ui.graphics.Color
import com.georgevik.turnia.core.domain.model.EventTypeId
import com.georgevik.turnia.ui.components.daydetail.components.EventTypeChipUi

sealed interface EventTypeDetailUi {
    data class Success(
        val title: EventTypeTitle,
        val form: EventTypeForm,
        val colors: List<Color>,
        val toastError: EventTypeToastError? = null,
        val formErrors: FormErrors = FormErrors(),
        val saveButtonLoading: Boolean = false,
        val isSaved: Boolean = false,
    ) : EventTypeDetailUi

    data class Error(val error: EventTypeScreenError) : EventTypeDetailUi
    data object Loading : EventTypeDetailUi

    data class EventTypeForm(
        val typeId: EventTypeId?,
        val fieldsEditable: Boolean,
        val name: String,
        val acronym: String,
        val description: String,
        val startTime: String,
        val endTime: String,
        val color: Color,
        val isGroupType: Boolean = false,
        val swappable: Boolean?,
    ) {
        val chipUi = EventTypeChipUi(title = acronym.ifEmpty { "   " }, color = color)
    }

    data class FormErrors(
        val nameError: EventTypeFieldError? = null,
        val acronymError: EventTypeFieldError? = null,
    )
}

enum class EventTypeScreenError {
    GroupNotFound,
    GroupEventNotFound
}

sealed interface EventTypeTitle{
    data class Title(val title: String) : EventTypeTitle
    data object New : EventTypeTitle
}

enum class EventTypeFieldError { Required }

enum class EventTypeToastError {
    PickColor, NotImplemented, SavePersonal, SaveGroup
}
