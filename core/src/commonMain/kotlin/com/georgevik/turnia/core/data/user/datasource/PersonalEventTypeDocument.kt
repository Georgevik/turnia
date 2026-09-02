package com.georgevik.turnia.core.data.user.datasource

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PersonalEventTypeDocument(
    @SerialName("name") val name: String,
    @SerialName("color") val color: String,
    @SerialName("acronym") val acronym: String,
    @SerialName("description") val description: String?,
    @SerialName("startTime") val startTime: String?,
    @SerialName("endTime") val endTime: String?,
)
