package com.georgevik.turnia.core.domain.model

import kotlinx.datetime.LocalDate

/**
 * An instance of a [PersonalEventType] on a date. Mirrors
 * `users/{ownerId}/personalEvents/{id}`.
 *
 * [notes] live on the event (personal events are private to the owner and their
 * shared users, so free-text notes are allowed here).
 */
data class PersonalEvent(
    val id: String,
    val type: PersonalEventType,
    val date: LocalDate,
    val notes: String?,
)
