package com.georgevik.turnia.ui.main.eventtypes.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.georgevik.turnia.navigation.EventTypeKind

@Immutable
data class EventTypeDetailUi(
    val kind: EventTypeKind,
    val isCreate: Boolean,
    val fieldsEditable: Boolean,
    val name: String,
    val acronym: String,
    val description: String,
    val startTime: String,
    val endTime: String,
    val color: Color,
    val swappable: Boolean?,
)
