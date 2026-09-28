package com.geoviksoft.turnia.core.data.datasource.firestore.doc

import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `users/{uid}/groupEventExtras/{eventId}`: what only this user keeps about a group event. */
@Serializable
data class GroupEventExtrasDocument(
    @SerialName("groupId") val groupId: String,
    @SerialName(FIELD_YEAR_MONTH) val yearMonth: String,
    /** `null` once cleared: the document stays, so other devices' delta sync sees the note go. */
    @SerialName("notes") val notes: String?,
    @SerialName(FIELD_UPDATE_AT) val updateAt: BaseTimestamp? = Timestamp.ServerTimestamp,
) {
    companion object {
        const val FIELD_YEAR_MONTH: String = "yearMonth"
        const val FIELD_UPDATE_AT: String = "updateAt"
    }
}
