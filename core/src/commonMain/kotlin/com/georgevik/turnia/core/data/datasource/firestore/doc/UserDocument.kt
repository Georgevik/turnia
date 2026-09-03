package com.georgevik.turnia.core.data.datasource.firestore.doc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The public part of a user: readable by everyone they share their calendar with, so it holds only
 * the name. `calendarSharedWith` stays here because the grant has to be queryable and the security
 * rules read it to authorize the very access it grants — everything else lives under `private`.
 */
@Serializable
data class UserDocument(
    @SerialName(FIELD_NAME) val name: String,
    @SerialName(FIELD_USERNAME) val username: String = "",
    @SerialName(FIELD_CALENDAR_SHARED_WITH) val calendarSharedWith: List<String> = emptyList(),
) {
    companion object {
        const val FIELD_NAME = "name"
        const val FIELD_USERNAME = "username"
        const val FIELD_CALENDAR_SHARED_WITH = "calendarSharedWith"
    }
}
