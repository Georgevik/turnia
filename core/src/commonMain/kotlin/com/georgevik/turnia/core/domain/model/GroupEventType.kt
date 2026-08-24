package com.georgevik.turnia.core.domain.model

/**
 * Template for a group's events (e.g. "Morning", "Night", "On-call"). Mirrors an
 * element of `groups/{groupId}.groupEventTypes[]`.
 *
 * Has **no color**: each user colors it via `users/{uid}.groupEventTypeColors`.
 */
data class GroupEventType(
    val id: String,
    val name: String,
    val acronym: String?,
    val description: String?,
    val startTime: String?,
    val endTime: String?,
)
