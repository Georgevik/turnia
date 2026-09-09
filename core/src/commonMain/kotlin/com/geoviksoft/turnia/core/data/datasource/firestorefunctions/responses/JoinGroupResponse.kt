package com.geoviksoft.turnia.core.data.datasource.firestorefunctions.responses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JoinGroupResponse(
    @SerialName("groupId") val groupId: String,
    @SerialName("status") val status: String,
) {
    companion object {
        const val STATUS_JOINED = "joined"
        const val STATUS_REQUESTED = "requested"
        const val STATUS_ALREADY_MEMBER = "already_member"
    }
}
