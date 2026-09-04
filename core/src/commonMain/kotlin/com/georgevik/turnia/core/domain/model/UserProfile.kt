package com.georgevik.turnia.core.domain.model

/** What any user allowed to see this one can read: nothing but the name. */
data class UserProfile(
    val id: UserId,
    val name: String,
    val username: String,
)
