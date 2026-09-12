package com.geoviksoft.turnia.ui.main.settings

import com.geoviksoft.turnia.core.domain.model.UserProfile

data class SettingsMenuUi(
    val userDetails: UserDetails? = null,
    val adsEnabled: Boolean = false,
) {
    val showPlanBanner: Boolean get() = userDetails?.isPremium == true || adsEnabled

    data class UserDetails(
        val displayName: String,
        val username: String,
        val isPremium: Boolean,
        val avatar: UserProfile.AnimalAvatar,
    )
}
