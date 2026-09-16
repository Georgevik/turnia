package com.geoviksoft.turnia.core.domain.model

/**
 * Remote feature flags, downloaded on the splash screen and read across the app.
 */
data class FeatureFlags(
    /** How many actions a free user does before the banner appears; negative turns ads off. */
    val minActionsToEnableAds: Int,
    val invitationCodeLength: Int,
    val enableSubscription: Boolean,
    val supportEmail: String,
) {
    val enableAds = minActionsToEnableAds >= 0
}
