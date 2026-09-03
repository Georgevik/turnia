package com.georgevik.turnia.core.domain.model

data class User(
    val uid: String,
    val firebaseUid: String,
    val email: String?,
    val displayName: String?,
    val username: String,
    val membership: Membership
)

enum class Membership {
    FREE, PREMIUM
}
