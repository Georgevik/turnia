package com.geoviksoft.turnia.core.domain.model

/**
 * Remote feature flags, downloaded on the splash screen and read across the app.
 *
 * @param enableAds whether AdMob ads are shown to free-tier users.
 * @param invitationCodeLength how many characters a freshly minted invitation code has. Remote so
 *   it can be raised without a release; codes already handed out keep whatever length they were
 *   born with, so changing it is safe at any time.
 */
data class FeatureFlags(
    val enableAds: Boolean,
    val invitationCodeLength: Int,
) {
    companion object {
        /** Conservative defaults used until the real flags are downloaded. */
        val Default = FeatureFlags(
            enableAds = false,
            invitationCodeLength = DEFAULT_INVITATION_CODE_LENGTH,
        )

        private const val DEFAULT_INVITATION_CODE_LENGTH = 6
    }
}
