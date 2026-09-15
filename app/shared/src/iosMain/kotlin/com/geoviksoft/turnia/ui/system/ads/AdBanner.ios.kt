package com.geoviksoft.turnia.ui.system.ads

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView

@Composable
internal actual fun PlatformAdBanner(modifier: Modifier, onLoaded: () -> Unit) {
    val factory = adBannerFactory ?: return
    val currentOnLoaded by rememberUpdatedState(onLoaded)

    BoxWithConstraints(modifier) {
        val width = maxWidth.value.toDouble()
        val height = remember(width) { factory.height(width) }
        UIKitView(
            factory = { factory.create(width) { currentOnLoaded() } },
            // Its full height even in a collapsed slot: the SDK serves nothing to a banner with no
            // height. It stays transparent until an ad arrives, so the overflow is never seen.
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(align = Alignment.Top, unbounded = true)
                .height(height.dp),
        )
    }
}

@Composable
internal actual fun rememberAdConsentPlatform(): AdConsentPlatform? = adConsentPlatform
