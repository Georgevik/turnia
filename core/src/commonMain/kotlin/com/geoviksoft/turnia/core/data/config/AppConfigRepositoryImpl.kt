package com.geoviksoft.turnia.core.data.config

import com.geoviksoft.turnia.core.domain.model.FeatureFlags
import com.geoviksoft.turnia.core.domain.repository.AppConfigRepository
import com.geoviksoft.turnia.core.system.outcomeCatching
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [AppConfigRepository] backed by Firebase Remote Config.
 *
 * Remote Config keeps the values it last activated on the device, so a launch with no network
 * starts from the flags as they were last seen rather than from the defaults.
 */
class AppConfigRepositoryImpl(
    private val remoteConfigService: RemoteConfigService,
) : AppConfigRepository {

    private val _featureFlags = MutableStateFlow(remoteConfigService.getFlags())
    override val featureFlags: StateFlow<FeatureFlags> = _featureFlags.asStateFlow()

    override suspend fun refreshFeatureFlags(): FeatureFlags {
        outcomeCatching(TAG, mapError = {}) {
            remoteConfigService.init()
            _featureFlags.value = remoteConfigService.getFlags()

            remoteConfigService.refresh()
            _featureFlags.value = remoteConfigService.getFlags()
        }
        return _featureFlags.value
    }


    companion object {
        private const val TAG = "AppConfigRepository"
    }
}
