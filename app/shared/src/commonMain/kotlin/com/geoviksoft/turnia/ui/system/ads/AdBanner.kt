package com.geoviksoft.turnia.ui.system.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.geoviksoft.turnia.core.domain.repository.AdRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.koin.compose.viewmodel.koinViewModel

/** An anchored banner that takes no space at all until [AdRepository] says it belongs there. */
@Composable
fun AdBanner(
    modifier: Modifier = Modifier,
    viewModel: AdBannerViewModel = koinViewModel(),
) {
    val visible by viewModel.visible.collectAsStateWithLifecycle()
    if (visible) {
        PlatformAdBanner(modifier.fillMaxWidth())
    }
}

class AdBannerViewModel(adRepository: AdRepository) : ViewModel() {
    val visible: StateFlow<Boolean> = adRepository.bannerVisible
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = false)
}

/** The store's own banner view, sized to the width it is given. */
@Composable
internal expect fun PlatformAdBanner(modifier: Modifier)
