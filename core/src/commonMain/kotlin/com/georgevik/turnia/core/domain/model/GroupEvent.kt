package com.georgevik.turnia.core.domain.model

import kotlinx.datetime.LocalDate

data class GroupEvent(
    val id: EventId,
    val groupId: GroupId,
    val groupName: String,
    val ownerId: UserId,
    val assigneeId: UserId,
    val assigneeName: String,
    val type: GroupEventType,
    val date: LocalDate,
    val onSwap: Boolean,
    val colorHex: String,
    val history: List<EventHistoryEntry>,
)
