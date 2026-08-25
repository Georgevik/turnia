package com.georgevik.turnia.ui.main.eventtypes.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.georgevik.turnia.navigation.EventTypeKind

@Immutable
data class EventTypeMasterRowUi(
    val kind: EventTypeKind,
    val groupId: String?,
    val groupName: String,
    val typeId: String,
    val acronym: String?,
    val color: Color,
) {
    val textColor: Color = if (color.luminance() > 0.5f) Color.Black else Color.White
}
