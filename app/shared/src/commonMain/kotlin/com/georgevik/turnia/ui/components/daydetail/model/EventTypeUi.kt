package com.georgevik.turnia.ui.components.daydetail.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.georgevik.turnia.core.domain.model.EventType

/** A group or personal [EventType] rendered as a pickable chip; both look the same on screen. */
@Immutable
data class EventTypeUi(
    val id: String,
    val title: String,
    val color: Color,
    val eventType: EventType,
) {
    val textColor: Color = if (color.luminance() > 0.5f) Color.Black else Color.White
}
