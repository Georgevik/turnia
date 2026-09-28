package com.geoviksoft.turnia.core.domain.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeatureFlagsTest {

    private val flags = FeatureFlags(
        minActionsToEnableAds = -1,
        invitationCodeLength = 6,
        enableSubscription = false,
        supportEmail = "",
    )

    @Test
    fun teamPromptShipsOff() {
        assertFalse(flags.teamPromptActive)
    }

    @Test
    fun teamPromptOnWithTheDefaultThreshold() {
        assertTrue(flags.copy(teamPromptEnabled = true).teamPromptActive)
    }

    @Test
    fun aThresholdBelowOneTurnsTheTeamPromptOff() {
        assertFalse(flags.copy(teamPromptEnabled = true, teamPromptThreshold = 0).teamPromptActive)
    }
}
