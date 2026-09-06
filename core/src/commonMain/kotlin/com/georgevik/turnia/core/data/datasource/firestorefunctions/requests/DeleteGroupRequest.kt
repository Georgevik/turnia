package com.georgevik.turnia.core.data.datasource.firestorefunctions.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeleteGroupRequest(
    @SerialName("groupId") val groupId: String,
)
