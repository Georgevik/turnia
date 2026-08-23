package com.georgevik.turnia.ui.main

import androidx.lifecycle.ViewModel
import com.georgevik.turnia.core.domain.model.FeatureFlags
import com.georgevik.turnia.core.domain.repository.AppConfigRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * Holds app-wide state the bottom nav needs — currently the feature flags that
 * decide which tabs are visible (downloaded earlier on the splash screen).
 */
class MainViewModel(
    appConfigRepository: AppConfigRepository,
) : ViewModel() {
    val featureFlags: StateFlow<FeatureFlags> = appConfigRepository.featureFlags
}
