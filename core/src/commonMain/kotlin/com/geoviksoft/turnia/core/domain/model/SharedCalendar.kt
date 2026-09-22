package com.geoviksoft.turnia.core.domain.model

/**
 * Another user's calendar for a date range: their shifts across every group they belong to, and
 * their personal events. Read whole and not stored — a group's events are unreadable to anyone
 * outside it, so this is aggregated on demand rather than mirrored anywhere.
 */
data class SharedCalendar(
    val groupEvents: List<GroupEvent>,
    val personalEvents: List<PersonalEvent>,
)

enum class SharedCalendarError {
    /** The owner has not granted this user access, or withdrew it. */
    NotShared,

    /** More days were asked for than the aggregation is allowed to gather at once. */
    RangeTooWide,

    LoadFailed,
}
