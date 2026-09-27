package com.geoviksoft.turnia.core.domain.model

/**
 * The link the share prompt hands out: the landing page, which sends each visitor to their own
 * store, tagged so an install can be traced back to the prompt.
 *
 * The root of the site and not `/join/`: only `/join/` opens an installed app (`AndroidManifest.xml`,
 * `apple-app-site-association`), and this link is meant for people who do not have it yet. The
 * landing page hands the `utm_*` tags to Google Play's install referrer, which is what lets Analytics
 * attribute the install on Android.
 */
object ShareLink {

    fun of(audience: SharePromptAudience): String =
        "https://${InvitationLinkConfig.HOST}/?utm_source=$SOURCE&utm_medium=$MEDIUM&utm_campaign=${audience.value}"

    private const val SOURCE = "turnia_share"
    private const val MEDIUM = "share_prompt"
}
