package com.geoviksoft.turnia.core.domain.model

/** What a move to a group did: events that became group shifts, and events left personal because the day was taken. */
data class MoveResult(val moved: Int, val skipped: Int)

enum class MoveError {
    /** Moving a single event onto a day where the user already holds a shift of that group. */
    DayTaken,
    Failed,
}
