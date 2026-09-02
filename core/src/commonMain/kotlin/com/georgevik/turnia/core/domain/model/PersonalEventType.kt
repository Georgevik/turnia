package com.georgevik.turnia.core.domain.model

data class PersonalEventType(
    val id: String,
    val name: String,
    val color: String,
    val acronym: String,
    val description: String?,
    val startTime: String?,
    val endTime: String?,
)
