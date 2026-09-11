package com.geoviksoft.turnia.link

import com.geoviksoft.turnia.core.domain.repository.InvitationLinkRepository
import org.koin.mp.KoinPlatform

/**
 * What SwiftUI's `onOpenURL` calls with a URL the app was opened with — the iOS counterpart of
 * `MainActivity.openLink`. Exposed as `InvitationLinkBridgeKt.onLinkOpened(link:)`.
 *
 * Every URL comes through here, the Google sign-in callback excepted: the repository keeps the
 * invitation links and ignores the rest.
 */
fun onLinkOpened(link: String) {
    KoinPlatform.getKoin().get<InvitationLinkRepository>().opened(link)
}
