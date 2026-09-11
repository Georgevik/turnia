package com.geoviksoft.turnia.core.domain.model

/**
 * Where an invitation link points: `https://HOST/PATH/CODE`, and `SCHEME://PATH/CODE`, which the
 * landing page opens an installed app with.
 *
 * Files outside Kotlin cannot read this and repeat the values; change them together:
 * `AndroidManifest.xml`, `Info.plist`, the entitlements, and `firebase/hosting` (`join.html`,
 * `apple-app-site-association`, and the rewrite in `firebase.json`).
 */
object InvitationLinkConfig {
    const val HOST = "turnia.club"
    const val SCHEME = "turnia"
    const val PATH = "join"
}
