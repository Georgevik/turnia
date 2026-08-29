package com.georgevik.turnia.ui.main.eventtypes.model

import androidx.compose.runtime.Immutable

@Immutable
data class EventTypeMasterUi(
    val query: String = "",
    val sections: List<EventTypeMasterHeaderUi> = emptyList(),
)
