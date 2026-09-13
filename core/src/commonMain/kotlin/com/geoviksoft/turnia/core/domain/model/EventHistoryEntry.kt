package com.geoviksoft.turnia.core.domain.model

/**
 * One step of a shift's chain: the shift reaching [userId].
 *
 * [returned] tells the two ways it can: taken from the previous holder (false), or handed back to
 * [userId] by whoever had taken it from them (true).
 */
data class EventHistoryEntry(
    val userId: UserId,
    val userName: String,
    val returned: Boolean = false,
)
