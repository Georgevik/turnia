package com.georgevik.turnia.core.data.datasource.firestore.doc

import dev.gitlive.firebase.firestore.BaseTimestamp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `users/{uid}/revokedGroups/{groupId}` — what is left of a group the user was removed from.
 *
 * They can no longer read `groups/{groupId}`: it carries the member roster and the invitation code,
 * and Firestore hides no fields. So the little they still need to render the events that stayed
 * assigned to them is copied here, on a document only they can read — the group's name, and only
 * the [groupEventTypes] those events actually use, plus the colour so the group still looks like
 * itself on their calendar.
 *
 * A frozen snapshot on purpose: nothing keeps it in step with the group afterwards.
 */
@Serializable
data class RevokedGroupDocument(
    @SerialName("name") val name: String,
    @SerialName("color") val color: String? = null,
    @SerialName("groupEventTypes") val groupEventTypes: List<GroupEventTypeDocument> = emptyList(),
    @SerialName("revokedAt") val revokedAt: BaseTimestamp? = null,
    @SerialName(FIELD_IS_DELETED) val isDeleted: Boolean = false,
    @SerialName(FIELD_UPDATE_AT) val updateAt: BaseTimestamp? = null,
) {
    companion object {
        const val FIELD_IS_DELETED = "isDeleted"
        const val FIELD_UPDATE_AT = "updateAt"
    }
}
