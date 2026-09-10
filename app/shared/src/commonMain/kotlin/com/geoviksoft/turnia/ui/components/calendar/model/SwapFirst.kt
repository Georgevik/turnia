package com.geoviksoft.turnia.ui.components.calendar.model

import kotlinx.datetime.LocalDate

/**
 * Offered shifts first, so one is never the event a cell hides.
 *
 * A cell draws two events and collapses the rest into "•••", and a shift going spare is the one
 * thing on the grid somebody has to act on — losing it to the overflow defeats the marker.
 *
 * A total ordering, not just `sortedByDescending { onSwap }`. A stable sort preserves the input
 * order, and the input order is not itself stable: `GroupEventFirestore` appends the events a sync
 * brought back after the ones it already held, so a day's tiles would swap places the moment one of
 * them changed. Sorting down to the id is what stops that.
 */
fun Map<LocalDate, List<DayEventUi>>.swapFirst(): Map<LocalDate, List<DayEventUi>> =
    mapValues { (_, events) ->
        events.sortedWith(
            compareByDescending<DayEventUi> { it.onSwap }
                .thenBy { it.timeRange.orEmpty() }
                .thenBy { it.id.value },
        )
    }
