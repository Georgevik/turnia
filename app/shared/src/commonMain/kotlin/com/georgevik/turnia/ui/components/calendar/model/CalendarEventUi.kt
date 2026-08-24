package com.georgevik.turnia.ui.components.calendar.model

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
 * @param name full event name (e.g. "Guardia noche").
 * @param acronym optional short siglas (e.g. "GN") the user set for the event;
 *   preferred on the tight calendar grid. `null`/blank means fall back to [name].
 * @param background cell chip fill and the sheet card's accent bar color.
 * @param textColor readable on-color for [background] (from its luminance).
 * @param timeRange e.g. "20:00 - 08:00", or `null` for an all-day event.
 * @param subtitle owner/description line shown in the details sheet.
 * @param onSwap group event offered for another member to take it over.
 * @param isOwner the current user created it (can manage it).
 * @param transferChain ordered names of every holder from the owner to the
 *   current performer (owner → … → assignee). Empty (or a single name) when the
 *   event was never handed over; two or more names means it's been ceded, which
 *   drives the "Cedido a X" chip, the diagonal hatch and the history trail.
 */
@Immutable
data class CalendarEventUi(
    val id: String,
    val type: CalendarEventType,
    val name: String,
    val acronym: String?,
    val background: Color,
    val textColor: Color,
    val timeRange: String?,
    val subtitle: String,
    val onSwap: Boolean,
    val isOwner: Boolean,
    val transferChain: List<String>,
) {
    /** What the calendar grid chip shows: the siglas if set, else the full name. */
    val gridLabel: String get() = acronym?.takeIf { it.isNotBlank() } ?: name

    /** Owned by the current user but performed by someone else — hatch it. */
    val assignedToOther: Boolean get() = transferChain.size >= 2

    /** Name of the current performer when it differs from the owner, else `null`. */
    val assigneeName: String? get() = transferChain.takeIf { it.size >= 2 }?.last()

    companion object {
        /**
         * Builds an event, choosing a light or dark [textColor] from the
         * relative luminance of [background] so the label stays legible.
         */
        fun create(
            id: String,
            type: CalendarEventType,
            name: String,
            background: Color,
            acronym: String? = null,
            timeRange: String? = null,
            subtitle: String = "",
            onSwap: Boolean = false,
            isOwner: Boolean = false,
            transferChain: List<String> = emptyList(),
        ): CalendarEventUi = CalendarEventUi(
            id = id,
            type = type,
            name = name,
            acronym = acronym,
            background = background,
            textColor = if (background.luminance() > 0.5f) Color.Black else Color.White,
            timeRange = timeRange,
            subtitle = subtitle,
            onSwap = onSwap,
            isOwner = isOwner,
            transferChain = transferChain,
        )
    }
}
