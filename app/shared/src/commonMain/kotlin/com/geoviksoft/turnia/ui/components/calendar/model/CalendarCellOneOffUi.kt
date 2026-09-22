package com.geoviksoft.turnia.ui.components.calendar.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.geoviksoft.turnia.ui.components.daydetail.components.clockLabel
import com.geoviksoft.turnia.ui.components.daydetail.model.OneOffEventUi
import kotlinx.datetime.LocalDate

/**
 * A one-off event as a line in a month cell: a dot of its colour and a few characters. A cell is
 * too narrow for a name next to a time, so it shows the one that tells the day apart — the time it
 * starts, or the name on a day it has no start time of its own.
 */
@Immutable
data class CalendarCellOneOffUi(
    val id: String,
    val label: String,
    val color: Color,
    val timed: Boolean,
)

/** The event as [date]'s cell draws it. */
fun OneOffEventUi.cellFor(date: LocalDate): CalendarCellOneOffUi {
    val startsThatDay = !allDay && start.date == date
    return CalendarCellOneOffUi(
        id = id,
        label = name,
        color = color,
        timed = startsThatDay,
    )
}

/** Whole-day lines first, as a calendar lists them, then the rest by the time they start. */
fun List<OneOffEventUi>.cellsFor(date: LocalDate): List<CalendarCellOneOffUi> =
    map { it.cellFor(date) to it.start }
        .sortedWith(compareBy({ (cell, _) -> cell.timed }, { (_, start) -> start }))
        .map { (cell, _) -> cell }
