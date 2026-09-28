package com.geoviksoft.turnia.core.domain.analytics

import com.geoviksoft.turnia.core.domain.model.MoveScope
import kotlin.test.Test
import kotlin.test.assertEquals

/** The console keys on these literals: a renamed one would start a new series. */
class MoveToGroupEventsTest {

    @Test
    fun movedCarriesScopeAndCounts() {
        val event = AnalyticsEvent.PersonalEventsMoved(MoveScope.All, eventCount = 12, skippedCount = 2)

        assertEquals("personal_events_moved", event.name)
        assertEquals(
            mapOf<String, Any>("scope" to "all", "event_count" to 12L, "skipped_count" to 2L),
            event.parameters,
        )
        assertEquals("one", AnalyticsEvent.PersonalEventsMoved(MoveScope.One, 1, 0).parameters["scope"])
    }

    @Test
    fun groupNotesSavedCarriesNoParameter() {
        assertEquals("group_event_notes_saved", AnalyticsEvent.GroupEventNotesSaved.name)
        assertEquals(emptyMap(), AnalyticsEvent.GroupEventNotesSaved.parameters)
    }
}
