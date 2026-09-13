package com.geoviksoft.turnia.core.data.datasource.firestorefunctions.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReturnEventRequest(
    @SerialName("groupId") val groupId: String,
    @SerialName("eventId") val eventId: String,
)
