package com.georgevik.turnia.core.data.datasource.firestore.doc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PersonalEventDocument(
    @SerialName("typeId") val typeId: String,
    @SerialName("date") val date: String,
    @SerialName("notes") val notes: String?,
    @SerialName(FIELD_YEAR_MONTH) val yearMonth: String,
) {
    companion object {
        const val FIELD_YEAR_MONTH: String = "yearMonth"
    }
}
