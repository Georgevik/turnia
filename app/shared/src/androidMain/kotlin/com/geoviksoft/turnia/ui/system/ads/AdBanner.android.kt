package com.geoviksoft.turnia.ui.system.ads

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
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
internal actual fun PlatformAdBanner(modifier: Modifier) {
    val unitId = koinInject<AdBannerUnitId>().value

    BoxWithConstraints(modifier) {
        val width = maxWidth.value.toInt()
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                AdView(context).apply {
                    adUnitId = unitId
                    setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, width))
                    loadAd(AdRequest.Builder().build())
                }
            },
            onRelease = AdView::destroy,
        )
    }
}
