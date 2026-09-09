package com.geoviksoft.turnia.core.data.datasource.firestorefunctions.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateProfileRequest(
    @SerialName("name") val name: String,
    @SerialName("username") val username: String,
)
