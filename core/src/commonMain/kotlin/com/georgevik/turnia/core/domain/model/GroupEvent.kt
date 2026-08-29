package com.georgevik.turnia.core.domain.model

import kotlinx.datetime.LocalDate

data class GroupEvent(
    val id: String,
    val groupId: String,
    val ownerId: String,
    val assigneeId: String,
    val type: GroupEventType,
    val date: LocalDate,
    val onSwap: Boolean,
    val colorHex: String,
    val history: List<String>,
)
