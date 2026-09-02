package com.georgevik.turnia.core.domain.model

data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val membership: Membership,
)
