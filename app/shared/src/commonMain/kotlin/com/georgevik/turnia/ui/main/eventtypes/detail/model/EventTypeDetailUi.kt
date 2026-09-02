package com.georgevik.turnia.ui.main.eventtypes.detail.model

import androidx.compose.ui.graphics.Color

sealed interface EventTypeDetailUi {
    data class Success(
        val title: EventTypeTitle,
        val form: EventTypeForm,
        val colors: List<Color>,
        val toastError: EventTypeToastError? = null,
        val saveButtonLoading: Boolean = false
    ) : EventTypeDetailUi

    data class Error(val error: EventTypeScreenError) : EventTypeDetailUi
    data object Loading : EventTypeDetailUi

    data class EventTypeForm(
        val typeId: String?,
        val fieldsEditable: Boolean,
        val name: String,
        val acronym: String,
        val description: String,
        val startTime: String,
        val endTime: String,
        val color: Color,
        val swappable: Boolean?,
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

enum class EventTypeToastError {
    PickColor, NotImplemented, SavePersonal, NameIsEmpty
}
