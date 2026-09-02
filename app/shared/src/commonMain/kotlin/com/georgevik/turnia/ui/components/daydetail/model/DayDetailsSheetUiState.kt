package com.georgevik.turnia.ui.components.daydetail.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface DayDetailsSheetUi {
    data object Loading : DayDetailsSheetUi
    data class Success(val predefinedSections: List<EventTypeSectionUi>) : DayDetailsSheetUi
    data class Error(val error: DayDetailsSheetError) : DayDetailsSheetUi
}

enum class DayDetailsSheetError { LoadFailed }
