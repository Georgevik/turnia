package com.geoviksoft.turnia.core.data.datasource.firestore.doc

import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PersonalOneOffDocument(
    @SerialName(FIELD_NAME) val name: String,
    @SerialName(FIELD_COLOR) val color: String,
    @SerialName(FIELD_START) val start: String,
    @SerialName(FIELD_END) val end: String,
    @SerialName(FIELD_ALL_DAY) val allDay: Boolean,
    @SerialName(FIELD_NOTES) val notes: String?,
    @SerialName(FIELD_IS_DELETED) val isDeleted: Boolean = false,
    @SerialName(FIELD_YEAR_MONTH_START) val yearMonthStart: String,
    @SerialName(FIELD_YEAR_MONTH_END) val yearMonthEnd: String,
    @SerialName(FIELD_UPDATE_AT) val updateAt: BaseTimestamp? = Timestamp.ServerTimestamp,
) {
    companion object {
        const val FIELD_START: String = "start"
        const val FIELD_END: String = "end"
        const val FIELD_NAME: String = "name"
        const val FIELD_COLOR: String = "color"
        const val FIELD_ALL_DAY: String = "allDay"
        const val FIELD_NOTES: String = "notes"
        const val FIELD_YEAR_MONTH_START: String = "yearMonthStart"
        const val FIELD_YEAR_MONTH_END: String = "yearMonthEnd"
        const val FIELD_UPDATE_AT: String = "updateAt"
        const val FIELD_IS_DELETED: String = "isDeleted"
    }
}
