package com.geoviksoft.turnia.ui.components.calendar.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.geoviksoft.turnia.core.domain.model.EventId
import com.geoviksoft.turnia.ui.system.color.readableTextColor

/**
 * An event as a tile in the month grid, which is all a cell can hold: a few millimetres of colour
 * with a word on it.
 *
 * Everything a shift knows — who covers it, its notes, its chain of transfers, what this user is
 * allowed to do with it — belongs to [DayEventUi] and cannot be shown here, so none of it is
 * carried. That is the point: a tile has five things to decide and this is those five, which is
 * also what makes it previewable without inventing a whole shift.
 */
@Immutable
data class CalendarCellEventUi(
    val id: EventId,
    /** The acronym where there is one, since a tile is too small for a name. */
    val label: String,
    val background: Color,
    /** Turns the marker in the corner. */
    val onSwap: Boolean,
    /** Hatches the tile: this user created the shift but somebody else covers it now. */
    val assignedToOther: Boolean,
) {
    val textColor: Color = background.readableTextColor()
}
