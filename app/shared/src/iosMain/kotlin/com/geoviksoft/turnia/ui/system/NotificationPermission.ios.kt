package com.geoviksoft.turnia.ui.system

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplication
import platform.UIKit.registerForRemoteNotifications
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter

@Composable
actual fun RequestNotificationPermission() {
    LaunchedEffect(Unit) {
        val options =
            UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound

        UNUserNotificationCenter.currentNotificationCenter()
            .requestAuthorizationWithOptions(options) { granted, _ ->
                if (!granted) return@requestAuthorizationWithOptions

                // Registering with APNs is what eventually gives Firebase a token to hand out, and
                // UIKit insists on the main thread; the callback arrives on whichever thread the
                // system felt like using.
                NSOperationQueue.mainQueue.addOperationWithBlock {
                    UIApplication.sharedApplication.registerForRemoteNotifications()
                }
            }
    }
}
