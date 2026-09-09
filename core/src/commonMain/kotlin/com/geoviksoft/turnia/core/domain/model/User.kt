package com.geoviksoft.turnia.core.domain.model

data class User(
    val id: UserId,
    val email: String?,
    val displayName: String?,
    val username: String,
    val membership: Membership
)

enum class Membership {
    FREE, PREMIUM
}
