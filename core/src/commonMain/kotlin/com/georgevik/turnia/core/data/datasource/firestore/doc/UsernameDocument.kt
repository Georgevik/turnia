package com.georgevik.turnia.core.data.datasource.firestore.doc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `usernames/{username}` — the reservation that makes a username unique, and the only way to find a
 * user you cannot read yet. The document id **is** the username; the field repeats it so it can be
 * ordered and prefix-queried, which a document id cannot.
 *
 * It carries [name] so a search result can be rendered without reading `users/{uid}`, which stays
 * unreadable until that user shares their calendar.
 */
@Serializable
data class UsernameDocument(
    @SerialName(FIELD_USERNAME) val username: String,
    @SerialName(FIELD_UID) val uid: String,
    @SerialName("name") val name: String,
) {
    companion object {
        const val FIELD_USERNAME = "username"
        const val FIELD_UID = "uid"
    }
}
