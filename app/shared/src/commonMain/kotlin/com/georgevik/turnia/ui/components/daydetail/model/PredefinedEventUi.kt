package com.georgevik.turnia.ui.components.daydetail.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

@Immutable
data class PredefinedEventUi(
    val id: String,
    val name: String,
    val color: Color,
    val acronym: String? = null,
) {
    val textColor: Color = if (color.luminance() > 0.5f) Color.Black else Color.White
}
