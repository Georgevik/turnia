package com.geoviksoft.turnia.ui.components.daydetail.model

import androidx.compose.runtime.Immutable

@Immutable
data class EventTypeSectionUi(
    val source: Source,
    val events: List<EventTypeUi>,
) {
    sealed interface Source {
        data object Personal : Source

        data class Group(
            val groupId: String,
            val groupName: String,
            val isAdmin: Boolean,
        ) : Source
    }
}
