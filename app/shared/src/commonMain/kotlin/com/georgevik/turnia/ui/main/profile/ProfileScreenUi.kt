package com.georgevik.turnia.ui.main.profile

data class ProfileScreenUi(
    val userDetails: UserDetails? = null
) {
    data class UserDetails(
        val displayName: String,
        val username: String,
        val email: String,
        val isPremium: Boolean,
    )
}
