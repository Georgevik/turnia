package com.georgevik.turnia.ui.components.daydetail.model

import androidx.compose.runtime.Immutable

@Immutable
data class EventTypeSectionUi(
    val type: Type,
    val events: List<PredefinedEventUi>,
) {
    sealed interface Type {
        data object Personal : Type
        data class Group(var groupId: String, var groupName: String) : Type
    }
}
