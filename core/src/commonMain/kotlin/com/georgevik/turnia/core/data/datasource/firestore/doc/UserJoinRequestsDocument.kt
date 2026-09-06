package com.georgevik.turnia.core.data.datasource.firestore.doc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserJoinRequestsDocument(
    @SerialName(FIELD_GROUP_IDS) val groupIds: List<String> = emptyList(),
) {
    companion object {
        const val FIELD_GROUP_IDS = "groupIds"
    }
}
