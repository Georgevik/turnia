package com.geoviksoft.turnia.core.data.config

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.geoviksoft.turnia.core.domain.model.FeatureFlags
import com.geoviksoft.turnia.core.domain.repository.AppConfigRepository
import com.geoviksoft.turnia.core.system.outcomeCatching
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/**
 * [AppConfigRepository] backed by Firebase Remote Config.
 *
 * Remote Config keeps the values it last activated on the device, so a launch with no network
 * starts from the flags as they were last seen rather than from the defaults.
 */
class AppConfigRepositoryImpl(
    private val remoteConfigService: RemoteConfigService,
    private val dataStore: DataStore<Preferences>,
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

    override suspend fun isOnboardingSeen(): Boolean =
        dataStore.data.first()[ONBOARDING_SEEN] ?: false

    override suspend fun setOnboardingSeen(seen: Boolean) {
        dataStore.edit { it[ONBOARDING_SEEN] = seen }
    }

    companion object {
        private const val TAG = "AppConfigRepository"
        private val ONBOARDING_SEEN = booleanPreferencesKey("onboarding_seen")
    }
}
