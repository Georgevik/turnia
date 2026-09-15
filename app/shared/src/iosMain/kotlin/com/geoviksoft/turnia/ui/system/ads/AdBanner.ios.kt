package com.geoviksoft.turnia.ui.system.ads

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView

@Composable
internal actual fun PlatformAdBanner(modifier: Modifier) {
    val factory = adBannerFactory ?: return

    BoxWithConstraints(modifier) {
        val width = maxWidth.value.toDouble()
        val height = remember(width) { factory.height(width) }
        UIKitView(
            factory = { factory.create(width) },
            modifier = Modifier.fillMaxWidth().height(height.dp),
        )
    }
}
