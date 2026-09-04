package com.georgevik.turnia.core.data.datasource.firestore.doc

import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.Timestamp
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
    @SerialName(FIELD_IS_DELETED) val isDeleted: Boolean = false,
    @SerialName(FIELD_UPDATE_AT) val updateAt: BaseTimestamp? = Timestamp.ServerTimestamp,
) {
    companion object {
        const val FIELD_IS_DELETED = "isDeleted"
        const val FIELD_UPDATE_AT = "updateAt"
    }
}
