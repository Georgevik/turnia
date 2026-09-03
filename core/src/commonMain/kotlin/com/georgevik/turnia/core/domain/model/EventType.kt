package com.georgevik.turnia.core.domain.model

/**
 * The template an event is created from: either a [GroupEventType] the group admin defines
 * or a [PersonalEventType] the user defines for themselves.
 *
 * [color] is already resolved per user: a group type has no color of its own, so it exposes
 * the user's pick for that type.
 */
sealed interface EventType {
    val id: String
    val name: String
    val acronym: String?
    val description: String?
    val startTime: String?
    val endTime: String?
    val color: String
}
