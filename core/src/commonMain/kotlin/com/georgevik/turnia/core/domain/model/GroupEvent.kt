package com.georgevik.turnia.core.domain.model

import kotlinx.datetime.LocalDate

/**
 * A group event, stored under the member who currently performs it
 * (`groups/{groupId}/members/{assigneeId}/event/{id}`). It is the single source
 * of truth for group events.
 *
 * - [ownerId] is the immutable creator.
 * - [assigneeId] is the current performer / last taker (equals the path member).
 * - [onSwap] `true` means it is offered for another member to take it over.
 *   Turnia swaps shifts, it never sells them, hence the name.
 *
 * No color and no notes: the doc is readable by every group member, so private
 * data must not live here. Colors come from each user's `groupEventTypeColors`.
 */
data class GroupEvent(
    val id: String,
    val groupId: String,
    val ownerId: String,
    val assigneeId: String,
    val type: GroupEventType,
    val date: LocalDate,
    val onSwap: Boolean,
)
