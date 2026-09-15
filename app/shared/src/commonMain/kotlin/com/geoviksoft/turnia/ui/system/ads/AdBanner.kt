package com.geoviksoft.turnia.ui.system.ads

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geoviksoft.turnia.core.domain.repository.AdRepository
import org.koin.compose.viewmodel.koinViewModel

/**
 * An anchored banner that takes no space at all until [AdRepository] says it belongs there, the
 * user has answered the consent message where one is needed, and an ad has actually arrived: a
 * request with no fill would otherwise leave an empty band above the tab bar.
 *
 * The band opens and closes in two steps, because the two conditions cannot be told apart by the
 * same mechanism: whether the banner belongs on screen at all decides whether the view is composed,
 * while whether an ad has filled only decides the band's height — the view has to be in the tree
 * to load an ad, so it cannot wait for one to be composed.
 */
@Composable
fun AdBanner(
    modifier: Modifier = Modifier,
    viewModel: AdBannerViewModel = koinViewModel(),
) {
    val visible by viewModel.visible.collectAsStateWithLifecycle()
    val consent by viewModel.consent.collectAsStateWithLifecycle()

    val platform = rememberAdConsentPlatform()

    LaunchedEffect(platform, visible) { if (visible) platform?.let(viewModel::gatherConsent) }

    AnimatedVisibility(
        visible = platform != null && visible && consent?.canRequestAds == true,
        modifier = modifier,
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        var loaded by remember { mutableStateOf(false) }
        Box(Modifier.fillMaxWidth().clipToBounds().animateContentSize()) {
            PlatformAdBanner(
                // Laid out at zero height rather than left out: the view has to exist to load the
                // ad. How each platform keeps loading inside that slot is its own business.
                modifier = Modifier.fillMaxWidth()
                    .then(if (loaded) Modifier else Modifier.height(0.dp)),
                onLoaded = { loaded = true },
            )
        }
    }
}

/**
 * The store's own banner view, sized to the width it is given. [onLoaded] is called on every ad
 * that arrives; a later refresh that fails keeps the ad already on screen.
 */
@Composable
internal expect fun PlatformAdBanner(modifier: Modifier, onLoaded: () -> Unit)
