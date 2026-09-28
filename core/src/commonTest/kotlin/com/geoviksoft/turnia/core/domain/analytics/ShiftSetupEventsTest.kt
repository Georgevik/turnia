package com.geoviksoft.turnia.core.domain.analytics

import com.geoviksoft.turnia.core.domain.model.EventKind
import com.geoviksoft.turnia.core.domain.model.ShiftSetupVia
import kotlin.test.Test
import kotlin.test.assertEquals

/** The console keys on these literals: a renamed one would start a new series. */
class ShiftSetupEventsTest {

    @Test
    fun shownCarriesWhereItWasOpened() {
        val event = AnalyticsEvent.OnboardShiftShown(ShiftSetupVia.AddPane)

        assertEquals("onboard_shift_shown", event.name)
        assertEquals(mapOf<String, Any>("via" to "add_pane"), event.parameters)
    }

    @Test
    fun skippedCarriesInteractedAsText() {
        val event = AnalyticsEvent.OnboardShiftSkipped(ShiftSetupVia.Onboarding, interacted = true)

        assertEquals("onboard_shift_skipped", event.name)
        assertEquals(mapOf<String, Any>("via" to "onboarding", "interacted" to "true"), event.parameters)
    }

    @Test
    fun completedCarriesItsCounts() {
        val event = AnalyticsEvent.OnboardShiftCompleted(
            ShiftSetupVia.Onboarding,
            interacted = false,
            typeCount = 3,
            customTypeCount = 0,
        )

        assertEquals("onboard_shift_completed", event.name)
        assertEquals(
            mapOf<String, Any>(
                "via" to "onboarding",
                "interacted" to "false",
                "type_count" to 3L,
                "custom_type_count" to 0L,
            ),
            event.parameters,
        )
    }

    @Test
    fun firstEventAddedCarriesItsKind() {
        assertEquals("first_event_added", AnalyticsEvent.FirstEventAdded(EventKind.Typed).name)
        assertEquals(mapOf<String, Any>("kind" to "typed"), AnalyticsEvent.FirstEventAdded(EventKind.Typed).parameters)
        assertEquals(mapOf<String, Any>("kind" to "one_off"), AnalyticsEvent.FirstEventAdded(EventKind.OneOff).parameters)
    }
}
