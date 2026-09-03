package com.georgevik.turnia.ui.components.daydetail.model

import androidx.compose.runtime.Immutable

@Immutable
data class EventTypeSectionUi(
    val source: Source,
    val events: List<EventTypeUi>,
) {
    sealed interface Source {
        data object Personal : Source
        data class Group(var groupId: String, var groupName: String) : Source
    }
}
