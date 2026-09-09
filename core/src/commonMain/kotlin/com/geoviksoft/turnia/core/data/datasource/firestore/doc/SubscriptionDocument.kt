package com.geoviksoft.turnia.core.data.datasource.firestore.doc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class SubscriptionDocument(
    val tier: Tier,
    val plan: Plan? = null,
    val platform: SubscriptionPlatform? = null,
    val expiresAt: Instant? = null,
    val updatedAt: Instant? = null,
)

@Serializable
enum class Plan {
    @SerialName("MONTHLY")
    MONTHLY,

    @SerialName("ANNUAL")
    ANNUAL
}

@Serializable
enum class SubscriptionPlatform {
    @SerialName("PLAY_STORE")
    PLAY_STORE,

    @SerialName("APP_STORE")
    APP_STORE
}

@Serializable
enum class Tier {
    @SerialName("FREE")
    FREE,

    @SerialName("PREMIUM")
    PREMIUM
}
