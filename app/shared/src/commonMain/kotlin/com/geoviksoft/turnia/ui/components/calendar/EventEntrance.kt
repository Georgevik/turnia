package com.geoviksoft.turnia.ui.components.calendar

import androidx.compose.runtime.Stable
import androidx.compose.runtime.withFrameNanos
import kotlinx.datetime.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

const val EVENT_ENTRANCE_MS = 200

private val EVENT_ENTRANCE_STEP = 12.milliseconds

/**
 * Hands out entrance delays to the cells of one month grid, so chips that arrive together sweep in
 * row by row instead of all at once.
 *
 * "Together" means the same frame: every entrance effect launched by one composition waits for the
 * next frame and resumes with the same frame time, in composition order — which is grid order. A
 * cell updated later on its own starts a new batch and animates straight away, rather than inheriting
 * the slot its position would have had in a full month.
 */
@Stable
class EventEntranceStagger {
    private var batchFrame = -1L
    private val slots = mutableMapOf<LocalDate, Int>()

    suspend fun delayFor(date: LocalDate): Duration {
        val frame = withFrameNanos { it }
        if (frame != batchFrame) {
            batchFrame = frame
            slots.clear()
        }
        return EVENT_ENTRANCE_STEP * slots.getOrPut(date) { slots.size }
    }
}
