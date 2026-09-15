package com.geoviksoft.turnia.core.data.config

import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.core.domain.model.FeatureFlags
import com.geoviksoft.turnia.core.domain.repository.AppConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Mock [AppConfigRepository] used until the real remote-config backend exists.
 * Ads are on; every other flag keeps its default. Swap this out for a Firestore/Remote-Config-backed impl
 * without touching callers.
 */
class AppConfigRepositoryImpl : AppConfigRepository {

    private val _featureFlags = MutableStateFlow(FeatureFlags.Default)
    override val featureFlags: StateFlow<FeatureFlags> = _featureFlags.asStateFlow()

    override suspend fun refreshFeatureFlags(): FeatureFlags {
        // No backend yet — return the mocked flags.
        val flags = FeatureFlags.Default.copy(enableAds = true)
        _featureFlags.value = flags
        Logger.d(TAG, "Feature flags downloaded (mock): $flags")
        return flags
    }

    companion object {
        private const val TAG = "AppConfigRepository"
    }
}
