package com.geoviksoft.turnia.core.data.datasource.firestore.doc

import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The public profile: readable by any signed-in user, because a search result has to render for
 * someone who has never shared anything with the searcher. `calendarSharedWith` stays here because
 * the grant has to be queryable and the security rules read it to authorize the very access it
 * grants — everything a stranger must not see lives under `private`.
 */
@Serializable
data class UserDocument(
    @SerialName(FIELD_NAME) val name: String,
    @SerialName(FIELD_USERNAME) val username: String = "",
    /** The `animal_icon_*` drawable suffix; null until the user picks one. */
    @SerialName(FIELD_ANIMAL_ICON_ID) val animalIconId: String? = null,
    /** Hex from `ALL_COLORS`; null until the user picks one. */
    @SerialName(FIELD_BACKGROUND_COLOR) val backgroundColor: String? = null,
    @SerialName(FIELD_CALENDAR_SHARED_WITH) val calendarSharedWith: List<String> = emptyList(),
    // Nullable: a write reads back with the server timestamp unresolved until it is acknowledged.
    @SerialName(FIELD_UPDATE_AT) val updateAt: BaseTimestamp? = Timestamp.ServerTimestamp,
) {
    companion object {
        const val FIELD_NAME = "name"
        const val FIELD_USERNAME = "username"
        const val FIELD_ANIMAL_ICON_ID = "animalIconId"
        const val FIELD_BACKGROUND_COLOR = "backgroundColor"
        const val FIELD_CALENDAR_SHARED_WITH = "calendarSharedWith"
        const val FIELD_UPDATE_AT = "updateAt"
    }
}
