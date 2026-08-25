package com.georgevik.turnia.ui.main.eventtypes.model

import androidx.compose.runtime.Immutable
import com.georgevik.turnia.navigation.EventTypeKind

@Immutable
data class EventTypeMasterHeaderUi(
    val kind: EventTypeKind,
    val rows: List<EventTypeMasterRowUi>,
)
