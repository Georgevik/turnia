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
    /** Turns the share prompt on; off until the store it points at is live on the platform. */
    val sharePromptEnabled: Boolean = false,
    /** Events added at which the share prompt appears, ascending and positive. */
    val sharePromptMilestones: List<Int> = emptyList(),
) {
    val enableAds = minActionsToEnableAds >= 0

    val sharePromptActive = sharePromptEnabled && sharePromptMilestones.isNotEmpty()
}
