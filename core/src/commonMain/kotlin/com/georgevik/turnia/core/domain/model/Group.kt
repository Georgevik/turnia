package com.georgevik.turnia.core.domain.model

data class Group(
    val id: String,
    val name: String,
    val types: List<GroupEventType>,
)
