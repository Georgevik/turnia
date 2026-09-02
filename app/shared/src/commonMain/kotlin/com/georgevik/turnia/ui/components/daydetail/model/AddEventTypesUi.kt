package com.georgevik.turnia.ui.components.daydetail.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface AddEventTypesUi {
    data object Loading : AddEventTypesUi
    data class Success(val sections: List<EventTypeSectionUi>) : AddEventTypesUi
    data class Error(val error: AddEventTypesError) : AddEventTypesUi
}

enum class AddEventTypesError { LoadFailed }
