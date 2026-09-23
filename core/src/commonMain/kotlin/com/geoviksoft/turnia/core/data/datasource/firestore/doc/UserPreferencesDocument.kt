package com.geoviksoft.turnia.core.data.datasource.firestore.doc

import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `users/{uid}/private/preferences` — what the user has chosen for themselves, readable by nobody
 * else. It sits under `private` for the privacy: what colour somebody paints their own shifts is
 * their business, and it used to sit on the profile, which is now world-readable.
 *
 * Read through the `preferences` marker on `users/{uid}/sync/updates` rather than through a
 * listener of its own: the sync document is already listened to, so the marker arrives free, and
 * the colours themselves come from the cache until another of this user's devices moves it.
 */
@Serializable
data class UserPreferencesDocument(
    /** Colour per group event type, keyed `"{groupId}_{typeId}"`: the type carries the default. */
    @SerialName(FIELD_TYPE_COLORS) val groupEventTypeColors: Map<String, String> = emptyMap(),
    /** Uids whose shared calendar this user has hidden. Their grant stands; only this user stops seeing it. */
    @SerialName(FIELD_HIDDEN_SHARED_CALENDARS) val hiddenSharedCalendars: List<String> = emptyList(),
    // Nullable: a write reads back with the server timestamp unresolved until it is acknowledged.
    @SerialName(FIELD_UPDATE_AT) val updateAt: BaseTimestamp? = Timestamp.ServerTimestamp,
) {
    companion object {
        const val FIELD_TYPE_COLORS = "groupEventTypeColors"
        const val FIELD_HIDDEN_SHARED_CALENDARS = "hiddenSharedCalendars"
        const val FIELD_UPDATE_AT = "updateAt"

        fun typeColorKey(groupId: String, typeId: String) = "${groupId}_${typeId}"
    }
}
