package com.georgevik.turnia.ui.components.calendar

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** The kind of calendar event, mirroring the domain (group vs. personal). */
enum class CalendarEventType { GROUP, PERSONAL }

/**
 * A fully-resolved, render-ready calendar event. Everything the UI needs is
 * precomputed here (in the view model, off the Compose thread) so the cell and
 * the details sheet only draw.
 *
 * @param text short name shown as the cell chip and the sheet's title chip.
 * @param background cell chip fill and the sheet card's accent bar color.
 * @param textColor readable on-color for [background] (from its luminance).
 * @param timeRange e.g. "20:00 - 08:00", or `null` for an all-day event.
 * @param subtitle owner/description line shown in the details sheet.
 * @param onSale group event offered for another member to take.
 * @param isOwner the current user owns it (can manage it).
 */
@Immutable
data class CalendarEventUi(
    val id: String,
    val type: CalendarEventType,
    val text: String,
    val background: Color,
    val textColor: Color,
    val timeRange: String?,
    val subtitle: String,
    val onSale: Boolean,
    val isOwner: Boolean,
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
            timeRange: String? = null,
            subtitle: String = "",
            onSale: Boolean = false,
            isOwner: Boolean = false,
        ): CalendarEventUi = CalendarEventUi(
            id = id,
            type = type,
            text = text,
            background = background,
            textColor = if (background.luminance() > 0.5f) Color.Black else Color.White,
            timeRange = timeRange,
            subtitle = subtitle,
            onSale = onSale,
            isOwner = isOwner,
        )
    }
}
