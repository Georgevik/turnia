package com.geoviksoft.turnia.core.data.datasource.firestore.doc

import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PersonalEventDocument(
    @SerialName("typeId") val typeId: String,
    @SerialName("date") val date: String,
    @SerialName(FIELD_NOTES) val notes: String?,
    @SerialName(FIELD_IS_DELETED) val isDeleted: Boolean = false,
    @SerialName(FIELD_YEAR_MONTH) val yearMonth: String,
    @SerialName(FIELD_UPDATE_AT) val updateAt: BaseTimestamp? = Timestamp.ServerTimestamp,
) {
    companion object {
        const val FIELD_NOTES: String = "notes"
        const val FIELD_YEAR_MONTH: String = "yearMonth"
        const val FIELD_UPDATE_AT: String = "updateAt"
        const val FIELD_IS_DELETED: String = "isDeleted"
    }
}
