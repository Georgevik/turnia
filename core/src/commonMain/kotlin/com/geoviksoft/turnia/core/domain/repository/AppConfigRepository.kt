package com.geoviksoft.turnia.core.domain.repository

import com.geoviksoft.turnia.core.domain.model.FeatureFlags
import kotlinx.coroutines.flow.StateFlow

/**
 * Provides app-wide remote configuration (feature flags). The flags are
 * downloaded once on the splash screen and then read wherever they gate UI.
 */
interface AppConfigRepository {

    /** Latest known flags. Emits the last activated values, or the defaults, until a refresh completes. */
    val featureFlags: StateFlow<FeatureFlags>

    /** Downloads the feature flags and updates [featureFlags]. Called on splash. */
    suspend fun refreshFeatureFlags(): FeatureFlags
    suspend fun isOnboardingSeen(): Boolean
    suspend fun setOnboardingSeen(seen: Boolean)

    /**
     * Whether this device is done with the shift setup: skipped, completed, or the account turned out
     * to have something already. Kept on the device only, never on the account.
     */
    suspend fun isShiftSetupSettled(): Boolean
    suspend fun setShiftSetupSettled(settled: Boolean)
}
