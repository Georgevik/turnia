package com.geoviksoft.turnia.core.data.datasource.firestorefunctions.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AcceptJoinRequest(
    @SerialName("groupId") val groupId: String,
    @SerialName("uid") val uid: String,
)
