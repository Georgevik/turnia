package com.geoviksoft.turnia.core.domain.repository

import com.geoviksoft.turnia.core.domain.model.FeatureFlags
import kotlinx.coroutines.flow.StateFlow

/**
 * Provides app-wide remote configuration (feature flags). The flags are
 * downloaded once on the splash screen and then read wherever they gate UI.
 */
interface AppConfigRepository {

    /** Latest known flags. Emits [FeatureFlags.Default] until a refresh completes. */
    val featureFlags: StateFlow<FeatureFlags>

    /** Downloads the feature flags and updates [featureFlags]. Called on splash. */
    suspend fun refreshFeatureFlags(): FeatureFlags
}
