package com.georgevik.turnia.core.domain.model

/**
 * Remote feature flags, downloaded on the splash screen and read across the app.
 *
 * @param showSwapTab whether the shift-swap ("Cambios") tab is available.
 * @param enableAds whether AdMob ads are shown to free-tier users.
 */
data class FeatureFlags(
    val showSwapTab: Boolean,
    val enableAds: Boolean,
) {
    companion object {
        /** Conservative defaults used until the real flags are downloaded. */
        val Default = FeatureFlags(
            showSwapTab = false,
            enableAds = false,
        )
    }
}
