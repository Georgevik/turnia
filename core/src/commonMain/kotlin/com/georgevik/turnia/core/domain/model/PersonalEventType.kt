package com.georgevik.turnia.core.domain.model

data class PersonalEventType(
    override val id: EventTypeId,
    override val name: String,
    override val color: String,
    override val acronym: String,
    override val description: String?,
    override val startTime: String?,
    override val endTime: String?,
    override val isDeleted: Boolean = false,
) : EventType
