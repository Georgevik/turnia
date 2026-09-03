package com.georgevik.turnia.ui.components.calendar.model

import androidx.compose.ui.graphics.vector.ImageVector

data class ThreeDotsOption(
    val text: String,
    val leadingIcon: ImageVector? = null,
    val onClick: () -> Unit
)
