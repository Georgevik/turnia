package com.georgevik.turnia.core.data.datasource.firestore.doc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `users/{uid}/private/account` — readable and writable only by the user themselves.
 */
@Serializable
data class UserPrivateDocument(
    @SerialName(FIELD_EMAIL) val email: String,
    @SerialName(FIELD_FCM_TOKENS) val fcmTokens: List<String> = emptyList(),
    @SerialName(FIELD_NOTIFICATIONS_ENABLED) val notificationsEnabled: Boolean = true,
) {
    companion object {
        const val FIELD_EMAIL = "email"
        const val FIELD_FCM_TOKENS = "fcmTokens"
        const val FIELD_NOTIFICATIONS_ENABLED = "notificationsEnabled"
    }
}
