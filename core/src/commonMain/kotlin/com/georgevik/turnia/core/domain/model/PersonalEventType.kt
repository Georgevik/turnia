package com.georgevik.turnia.core.domain.model

/**
 * A user-defined personal event template. Mirrors
 * `users/{uid}/personalEventTypes/{typeId}`. Unlike [GroupEventType] it carries
 * its own [color].
 */
data class PersonalEventType(
    val id: String,
    val name: String,
    val color: String,
    val acronym: String?,
    val description: String?,
    val startTime: String?,
    val endTime: String?,
)
