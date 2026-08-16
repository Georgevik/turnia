package com.georgevik.turnia.ui.components.calendar

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** The kind of calendar event, mirroring the domain (group vs. personal). */
enum class CalendarEventType { GROUP, PERSONAL }

/**
 * A fully-resolved, render-ready calendar event. Everything the UI needs is
 * precomputed here (in the view model, off the Compose thread) so the cell only
 * draws — notably [textColor], the readable on-color for [background].
 */
@Immutable
data class CalendarEventUi(
    val id: String,
    val type: CalendarEventType,
    val text: String,
    val background: Color,
    val textColor: Color,
) {
    companion object {
        /**
         * Builds an event, choosing a light or dark [textColor] from the
         * relative luminance of [background] so the label stays legible.
         */
        fun create(
            id: String,
            type: CalendarEventType,
            text: String,
            background: Color,
        ): CalendarEventUi = CalendarEventUi(
            id = id,
            type = type,
            text = text,
            background = background,
            textColor = if (background.luminance() > 0.5f) Color.Black else Color.White,
        )
    }
}
