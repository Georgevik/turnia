package com.georgevik.turnia.core.data.datasource.firestore.doc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `users/{uid}/private/account` — readable and writable only by the user themselves.
 */
@Serializable
data class UserPrivateDocument(
    @SerialName("email") val email: String,
    @SerialName("fcmTokens") val fcmTokens: List<String> = emptyList(),
)
