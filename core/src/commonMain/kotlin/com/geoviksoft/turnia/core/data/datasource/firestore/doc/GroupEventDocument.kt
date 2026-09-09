package com.geoviksoft.turnia.core.data.datasource.firestore.doc

import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `groups/{groupId}/events/{eventId}`
 */
@Serializable
data class GroupEventDocument(
    @SerialName(FIELD_OWNER_ID) val ownerId: String,
    @SerialName(FIELD_ASSIGNEE_ID) val assigneeId: String,
    @SerialName("groupEventTypeId") val groupEventTypeId: String,
    @SerialName("date") val date: String,
    @SerialName(FIELD_YEAR_MONTH) val yearMonth: String,
    @SerialName("onSwap") val onSwap: Boolean = false,
    @SerialName(FIELD_HISTORY) val history: List<EventHistoryDocument> = emptyList(),
    @SerialName(FIELD_IS_DELETED) val isDeleted: Boolean = false,
    // Nullable: a write reads back with the server timestamp unresolved until it is acknowledged.
    @SerialName(FIELD_UPDATE_AT) val updateAt: BaseTimestamp? = Timestamp.ServerTimestamp,
) {
    companion object {
        const val FIELD_OWNER_ID = "ownerId"
        const val FIELD_ASSIGNEE_ID = "assigneeId"
        const val FIELD_YEAR_MONTH = "yearMonth"
        const val FIELD_IS_DELETED = "isDeleted"
        const val FIELD_UPDATE_AT = "updateAt"
        const val FIELD_HISTORY = "history"
    }
}

@Serializable
data class EventHistoryDocument(
    @SerialName("type") val type: String,
    @SerialName("actorUid") val actorUid: String,
    @SerialName("fromUid") val fromUid: String? = null,
    @SerialName("toUid") val toUid: String? = null,
    @SerialName("timestamp") val timestamp: BaseTimestamp? = null,
) {
    companion object {
        const val TYPE_PUT_ON_SWAP = "put_on_swap"
        const val TYPE_TRANSFERRED = "transferred"
    }
}
