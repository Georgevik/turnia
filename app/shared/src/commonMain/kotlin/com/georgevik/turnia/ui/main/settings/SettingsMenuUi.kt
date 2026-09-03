package com.georgevik.turnia.ui.main.settings

data class SettingsMenuUi(
    val userDetails: UserDetails? = null
) {
    data class UserDetails(
        val displayName: String,
        val username: String,
        val email: String,
        val isPremium: Boolean,
    )
}
