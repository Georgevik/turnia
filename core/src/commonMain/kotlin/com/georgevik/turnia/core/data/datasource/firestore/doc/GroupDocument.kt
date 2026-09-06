package com.georgevik.turnia.core.data.datasource.firestore.doc

import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `groups/{groupId}` — the group and the event types its admin defines, in the same document: the
 * types are read together with the group, always, so splitting them into a subcollection would only
 * add a read.
 *
 * [memberUids] is here for the same reason: "the groups I belong to" is then one query that already
 * returns each group's event types, instead of finding the memberships and reading the groups after.
 */
@Serializable
data class GroupDocument(
    @SerialName("name") val name: String,
    @SerialName(FIELD_MEMBER_UIDS) val memberUids: List<String> = emptyList(),
    // Former members who still hold events here: they read only their own, and never this document.
    @SerialName(FIELD_REVOKED_UIDS) val revokedUids: List<String> = emptyList(),
    @SerialName("members") val members: Map<String, GroupMemberDocument> = emptyMap(),
    @SerialName("adminUids") val adminUids: List<String> = emptyList(),
    @SerialName("groupEventTypes") val groupEventTypes: List<GroupEventTypeDocument> = emptyList(),
    // Required, with no default: the code is the only way into a group, so a group without one
    // cannot be joined by anybody and there is nothing sensible to stand in for it. A document
    // written before that was true fails to deserialize, and the reader skips it.
    @SerialName("invitation") val invitation: InvitationDocument,
    @SerialName(FIELD_UPDATE_AT) val updateAt: BaseTimestamp? = Timestamp.ServerTimestamp,
) {
    companion object {
        const val FIELD_MEMBER_UIDS = "memberUids"
        const val FIELD_REVOKED_UIDS = "revokedUids"
        const val FIELD_UPDATE_AT = "updateAt"
    }
}

@Serializable
data class GroupMemberDocument(
    @SerialName("name") val name: String,
    @SerialName("username") val username: String,
)

/** No color: each user picks their own per type in `users/{uid}.groupEventTypeColors`. */
@Serializable
data class GroupEventTypeDocument(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("acronym") val acronym: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("startTime") val startTime: String? = null,
    @SerialName("endTime") val endTime: String? = null,
    @SerialName("swappable") val swappable: Boolean = true,
)

/**
 * [membersCanSeeCode] only hides the code in the UI: the group document is readable by every
 * member, so it is a house rule, not a boundary.
 */
@Serializable
data class InvitationDocument(
    @SerialName("code") val code: String,
    @SerialName("active") val active: Boolean = true,
    @SerialName("autoApprove") val autoApprove: Boolean = false,
    @SerialName("membersCanSeeCode") val membersCanSeeCode: Boolean = false,
    @SerialName("expiresAt") val expiresAt: BaseTimestamp? = null,
)
