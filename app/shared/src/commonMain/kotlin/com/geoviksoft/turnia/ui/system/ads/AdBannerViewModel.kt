package com.geoviksoft.turnia.ui.system.ads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoviksoft.turnia.core.domain.repository.AdRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class AdBannerViewModel(
    adRepository: AdRepository,
    private val adConsent: AdConsent,
) : ViewModel() {
    val visible: StateFlow<Boolean> = adRepository.bannerVisible
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = false)

    val consent: StateFlow<AdConsentStatus?> = adConsent.status

    fun gatherConsent(platform: AdConsentPlatform) = adConsent.gather(platform)
}
