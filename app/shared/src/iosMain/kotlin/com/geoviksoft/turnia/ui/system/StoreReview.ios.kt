package com.geoviksoft.turnia.ui.system

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSURL
import platform.StoreKit.SKStoreReviewController
import platform.UIKit.UIApplication
import platform.UIKit.UIWindowScene

/**
 * The numeric Apple ID from App Store Connect (App Information). The write-review page only works
 * once the app is live on the App Store; before that it shows the app as unavailable.
 */
private const val APP_STORE_ID: String = "6811986973"

@Composable
actual fun rememberStoreReview(): StoreReview = remember {
    object : StoreReview {
        override val store = AppStore.APP_STORE

        override fun open() {
            val url =
                NSURL.URLWithString("https://apps.apple.com/app/id$APP_STORE_ID?action=write-review")
            if (url != null) {
                UIApplication.sharedApplication.openURL(
                    url,
                    options = emptyMap<Any?, Any>(),
                    completionHandler = null
                )
                return
            }
            val scene =
                UIApplication.sharedApplication.connectedScenes.firstOrNull { it is UIWindowScene } as? UIWindowScene
                    ?: return
            SKStoreReviewController.requestReviewInScene(scene)
        }
    }
}
