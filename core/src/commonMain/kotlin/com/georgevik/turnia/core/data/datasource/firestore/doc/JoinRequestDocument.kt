package com.georgevik.turnia.core.data.datasource.firestore.doc

import dev.gitlive.firebase.firestore.BaseTimestamp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `groups/{groupId}/joinRequests/{uid}` — the document id is the requester's uid.
 *
 * The name travels with the request because `users/{uid}` is unreadable to an admin who does not
 * share a calendar with the requester: without it there is nobody to show.
 */
@Serializable
data class JoinRequestDocument(
    @SerialName("name") val name: String = "",
    @SerialName("username") val username: String = "",
    @SerialName("requestedAt") val requestedAt: BaseTimestamp? = null,
)
