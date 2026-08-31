package com.georgevik.turnia.ui.components.daydetail.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

@Immutable
data class PredefinedEventUi(
    val id: String,
    val title: String,
    val color: Color,
    val domainObject: Any
) {
    val textColor: Color = if (color.luminance() > 0.5f) Color.Black else Color.White
}
