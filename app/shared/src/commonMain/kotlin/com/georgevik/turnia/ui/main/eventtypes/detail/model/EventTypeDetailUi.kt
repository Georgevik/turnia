package com.georgevik.turnia.ui.main.eventtypes.detail.model

import androidx.compose.ui.graphics.Color


data class EventTypeDetailUi(
    val eventTypeForm: EventTypeForm?,
    val loading: Boolean
) {
    data class EventTypeForm(
        val typeId: String?,
        val fieldsEditable: Boolean,
        val name: String,
        val acronym: String,
        val description: String,
        val startTime: String,
        val endTime: String,
        val color: Color,
        val swappable: Boolean?,
    )
}
