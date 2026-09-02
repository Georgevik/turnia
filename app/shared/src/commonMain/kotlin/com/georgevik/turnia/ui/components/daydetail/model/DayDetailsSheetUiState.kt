package com.georgevik.turnia.ui.components.daydetail.model

import androidx.compose.runtime.Immutable

@Immutable
data class DayDetailsSheetUiState(
    val predefinedSections: List<EventTypeSectionUi> = emptyList(),
)
