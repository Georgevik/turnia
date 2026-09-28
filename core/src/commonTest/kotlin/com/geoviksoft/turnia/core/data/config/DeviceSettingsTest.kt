package com.geoviksoft.turnia.core.data.config

import com.geoviksoft.turnia.core.fakes.InMemoryDataStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeviceSettingsTest {

    private val settings = DeviceSettings(InMemoryDataStore())

    @Test
    fun theShiftSetupStartsUnsettled() = runTest {
        assertFalse(settings.isShiftSetupSettled())
    }

    @Test
    fun theShiftSetupFlagRoundTrips() = runTest {
        settings.setShiftSetupSettled(true)

        assertTrue(settings.isShiftSetupSettled())
        assertFalse(settings.isOnboardingSeen(), "Each flag keeps its own key")
    }
}
