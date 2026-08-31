package com.georgevik.turnia.core.domain.model

data class GroupEventType(
    val id: String,
    val groupId: String,
    val name: String,
    val acronym: String?,
    val description: String?,
    val startTime: String?,
    val endTime: String?,
    val swappable: Boolean,
    private val colorHex: String,
    private val userColor: String?
) {
    val color = userColor ?: colorHex
}
