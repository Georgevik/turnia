package com.geoviksoft.turnia.core.data.datasource.firestore.doc

import dev.gitlive.firebase.firestore.BaseTimestamp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserJoinRequestsDocument(
    @SerialName(FIELD_GROUP_IDS) val groupIds: List<String> = emptyList(),
    @SerialName(FIELD_UPDATE_AT) val updateAt: BaseTimestamp? = null,
) {
    companion object {
        const val FIELD_GROUP_IDS = "groupIds"
        const val FIELD_UPDATE_AT = "updateAt"
    }
}
