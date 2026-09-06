package com.georgevik.turnia.core.data.datasource.firestore.doc

import dev.gitlive.firebase.firestore.BaseTimestamp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `groups/{groupId}/joinRequests/{uid}` — the document id is the requester's uid.
 *
 * The name travels with the request because `users/{uid}` is unreadable to an admin who does not
 * share a calendar with the requester: without it there is nobody to show. The group's name travels
 * the other way for the same reason — a rejected requester never became a member, so they cannot
 * read `groups/{groupId}` to name the group that turned them down.
 */
@Serializable
data class JoinRequestDocument(
    @SerialName(FIELD_NAME) val name: String = "",
    @SerialName(FIELD_USERNAME) val username: String = "",
    @SerialName(FIELD_GROUP_NAME) val groupName: String = "",
    @SerialName(FIELD_STATUS) val status: JoinRequestStatusDocument = JoinRequestStatusDocument.PENDING,
    @SerialName(FIELD_REQUESTED_AT) val requestedAt: BaseTimestamp? = null,
    @SerialName(FIELD_RESPONDED_AT) val respondedAt: BaseTimestamp? = null,
) {
    companion object {
        const val FIELD_NAME = "name"
        const val FIELD_USERNAME = "username"
        const val FIELD_GROUP_NAME = "groupName"
        const val FIELD_STATUS = "status"
        const val FIELD_REQUESTED_AT = "requestedAt"
        const val FIELD_RESPONDED_AT = "respondedAt"
        const val STATUS_PENDING = "pending"
    }
}

@Serializable
enum class JoinRequestStatusDocument {
    @SerialName(JoinRequestDocument.STATUS_PENDING)
    PENDING,

    @SerialName("accepted")
    ACCEPTED,

    @SerialName("rejected")
    REJECTED,
}
