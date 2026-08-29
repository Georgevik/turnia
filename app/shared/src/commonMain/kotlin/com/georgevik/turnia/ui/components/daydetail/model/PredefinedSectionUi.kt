package com.georgevik.turnia.ui.components.daydetail.model

import androidx.compose.runtime.Immutable

@Immutable
data class PredefinedSectionUi(
    val groupId: String?,
    val groupName: String?,
    val events: List<PredefinedEventUi>,
)
