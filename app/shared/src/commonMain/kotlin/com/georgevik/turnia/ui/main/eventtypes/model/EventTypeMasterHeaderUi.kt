package com.georgevik.turnia.ui.main.eventtypes.model

import androidx.compose.runtime.Immutable
import com.georgevik.turnia.navigation.main.routes.EventTypeKind

@Immutable
data class EventTypeMasterHeaderUi(
    val kind: EventTypeKind,
    val name: String,
    val rows: List<EventTypeMasterRowUi>,
)
