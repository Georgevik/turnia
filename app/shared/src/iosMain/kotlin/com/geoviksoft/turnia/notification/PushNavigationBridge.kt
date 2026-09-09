package com.geoviksoft.turnia.notification

import com.geoviksoft.turnia.core.domain.repository.NotificationRepository
import org.koin.mp.KoinPlatform

/**
 * What `UNUserNotificationCenterDelegate` calls when the user taps a notification — the iOS
 * counterpart of `MainActivity.openNotification`.
 *
 * Exposed as `PushNavigationBridgeKt.onPushOpened(data:)` in the `Shared` framework. Swift narrows
 * `userInfo` to its string entries before calling: the payload also carries `aps`, a dictionary,
 * and nothing here has any use for it.
 */
fun onPushOpened(data: Map<String, String>) {
    KoinPlatform.getKoin().get<NotificationRepository>().opened(data)
}
