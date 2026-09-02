package com.georgevik.turnia.core.data.datasource.firestore.doc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDocument(
    @SerialName("name") val name: String,
    @SerialName("email") val email: String,
    @SerialName("fcmTokens") val fcmTokens: List<String>,
    @SerialName("subscription") val subscription: SubscriptionDocument,
)
