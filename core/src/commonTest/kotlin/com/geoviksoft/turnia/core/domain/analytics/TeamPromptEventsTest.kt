package com.geoviksoft.turnia.core.domain.analytics

import com.geoviksoft.turnia.core.domain.model.TeamPromptChoice
import kotlin.test.Test
import kotlin.test.assertEquals

/** The console keys on these literals: a renamed one would start a new series. */
class TeamPromptEventsTest {

    @Test
    fun shownCarriesNoParameter() {
        assertEquals("onboard_team_shown", AnalyticsEvent.OnboardTeamShown.name)
        assertEquals(emptyMap(), AnalyticsEvent.OnboardTeamShown.parameters)
    }

    @Test
    fun answeredCarriesTheChoice() {
        val expected = mapOf(
            TeamPromptChoice.Create to "create",
            TeamPromptChoice.Join to "join",
            TeamPromptChoice.Dismissed to "dismissed",
        )

        expected.forEach { (choice, value) ->
            val event = AnalyticsEvent.OnboardTeamAnswered(choice)
            assertEquals("onboard_team_answered", event.name)
            assertEquals(mapOf<String, Any>("choice" to value), event.parameters)
        }
    }
}
