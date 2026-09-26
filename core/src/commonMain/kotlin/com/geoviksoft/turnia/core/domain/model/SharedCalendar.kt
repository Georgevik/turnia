package com.geoviksoft.turnia.core.domain.model

/**
 * Another user's calendar around a month: their shifts across every group they belong to, and
 * their personal events. A group's events are unreadable to anyone outside it, so the server
 * aggregates it on demand and mirrors it nowhere. The viewer's device keeps each month, and asks
 * only for what changed once the owner's markers say it moved: that is also what keeps old months
 * after the server purges them.
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
