package com.geoviksoft.turnia.ui.system.ads

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import org.koin.compose.koinInject

/**
 * The banner's ad unit id. It is a resource of the app, which is the module with build types — test
 * in debug, real in release — so the app provides it rather than this library reading it.
 */
class AdBannerUnitId(val value: String)

@Composable
internal actual fun PlatformAdBanner(modifier: Modifier, onLoaded: () -> Unit) {
    val unitId = koinInject<AdBannerUnitId>().value
    val currentOnLoaded by rememberUpdatedState(onLoaded)

    BoxWithConstraints(modifier) {
        val width = maxWidth.value.toInt()
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                AdView(context).apply {
                    adUnitId = unitId
                    // Anchored, as on iOS: an inline size takes its height from the slot, and the
                    // bottom bar's slot is nearly the whole screen.
                    setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, width))
                    adListener = object : AdListener() {
                        override fun onAdLoaded() = currentOnLoaded()
                    }
                    loadAd(AdRequest.Builder().build())
                }
            },
            onRelease = AdView::destroy,
        )
    }
}
