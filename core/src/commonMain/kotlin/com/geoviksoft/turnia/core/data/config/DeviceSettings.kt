package com.geoviksoft.turnia.core.data.config

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first

/** The flags that belong to the device rather than the account, in the app's settings file. */
class DeviceSettings(private val dataStore: DataStore<Preferences>) {

    suspend fun isOnboardingSeen(): Boolean = dataStore.data.first()[ONBOARDING_SEEN] ?: false

    suspend fun setOnboardingSeen(seen: Boolean) {
        dataStore.edit { it[ONBOARDING_SEEN] = seen }
    }

    suspend fun isShiftSetupSettled(): Boolean = dataStore.data.first()[SHIFT_SETUP_SETTLED] ?: false

    suspend fun setShiftSetupSettled(settled: Boolean) {
        dataStore.edit { it[SHIFT_SETUP_SETTLED] = settled }
    }

    private companion object {
        val ONBOARDING_SEEN = booleanPreferencesKey("onboarding_seen")
        val SHIFT_SETUP_SETTLED = booleanPreferencesKey("shift_setup_settled")
    }
}
