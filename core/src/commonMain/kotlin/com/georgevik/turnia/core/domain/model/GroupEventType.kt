package com.georgevik.turnia.core.domain.model

data class GroupEventType(
    override val id: EventTypeId,
    val groupId: GroupId,
    val groupName: String,
    override val name: String,
    override val acronym: String?,
    override val description: String?,
    override val startTime: String?,
    override val endTime: String?,
    val swappable: Boolean,
    private val colorHex: String,
    private val userColor: String?,
    override val isDeleted: Boolean = false
) : EventType {
    override val color = userColor ?: colorHex
}
