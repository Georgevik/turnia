package com.georgevik.turnia.core.domain.model

data class GroupEventType(
    val id: String,
    val name: String,
    val acronym: String?,
    val description: String?,
    val startTime: String?,
    val endTime: String?,
    val swappable: Boolean,
)
