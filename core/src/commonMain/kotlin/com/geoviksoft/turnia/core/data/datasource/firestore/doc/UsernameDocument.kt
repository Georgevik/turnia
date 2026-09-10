package com.geoviksoft.turnia.core.data.datasource.firestore.doc

import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `usernames/{username}` — the reservation that makes a username unique, and the index a prefix
 * search runs over. The document id **is** the username; the field repeats it so it can be ordered
 * and prefix-queried, which a document id cannot.
 *
 * It carries no name: the profile it points at is readable by any signed-in user now. What it does
 * carry is [updateAt], the marker a searcher compares their cached `users/{uid}` against, so a
 * result already seen is rendered from the cache instead of billing a read.
 */
@Serializable
data class UsernameDocument(
    @SerialName(FIELD_USERNAME) val username: String,
    @SerialName(FIELD_UID) val uid: String,
    // Nullable: a write reads back with the server timestamp unresolved until it is acknowledged.
    @SerialName(FIELD_UPDATE_AT) val updateAt: BaseTimestamp? = Timestamp.ServerTimestamp,
) {
    companion object {
        const val FIELD_USERNAME = "username"
        const val FIELD_UID = "uid"
        const val FIELD_UPDATE_AT = "updateAt"
    }
}
