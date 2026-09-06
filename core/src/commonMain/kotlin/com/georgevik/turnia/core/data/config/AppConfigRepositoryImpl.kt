package com.georgevik.turnia.core.data.config

import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.FeatureFlags
import com.georgevik.turnia.core.domain.repository.AppConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Mock [AppConfigRepository] used until the real remote-config backend exists.
 * Every flag is off. Swap this out for a Firestore/Remote-Config-backed impl
 * without touching callers.
 */
class AppConfigRepositoryImpl : AppConfigRepository {

    private val _featureFlags = MutableStateFlow(FeatureFlags.Default)
    override val featureFlags: StateFlow<FeatureFlags> = _featureFlags.asStateFlow()

    override suspend fun refreshFeatureFlags(): FeatureFlags {
        // No backend yet — return the mocked flags (all disabled).
        val flags = FeatureFlags.Default
        _featureFlags.value = flags
        Logger.d(TAG, "Feature flags downloaded (mock): $flags")
        return flags
    }

    companion object {
        private const val TAG = "AppConfigRepository"
    }
}
