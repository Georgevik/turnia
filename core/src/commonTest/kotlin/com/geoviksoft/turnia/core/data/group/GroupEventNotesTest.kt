package com.geoviksoft.turnia.core.data.group

import com.geoviksoft.turnia.core.data.datasource.firestore.errors.GenericFirestoreError
import com.geoviksoft.turnia.core.domain.analytics.AnalyticsEvent
import com.geoviksoft.turnia.core.fakes.RecordingAnalytics
import com.geoviksoft.turnia.core.system.isSuccess
import com.geoviksoft.turnia.core.system.toFailure
import com.geoviksoft.turnia.core.system.toSuccess
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroupEventNotesTest {

    private val analytics = RecordingAnalytics()

    @Test
    fun aBlankNoteIsStoredAsNoNote() = runTest {
        var written: String? = "unset"

        saveGroupEventNote("   ", analytics) { note -> written = note; Unit.toSuccess() }

        assertEquals(null, written)
    }

    @Test
    fun aNoteIsTrimmed() = runTest {
        var written: String? = null

        saveGroupEventNote("  Parking B \n", analytics) { note -> written = note; Unit.toSuccess() }

        assertEquals("Parking B", written)
    }

    @Test
    fun aSuccessLogsOnce() = runTest {
        val outcome = saveGroupEventNote("Parking B", analytics) { Unit.toSuccess() }

        assertTrue(outcome.isSuccess)
        assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.GroupEventNotesSaved), analytics.events)
    }

    @Test
    fun aFailureLogsNothing() = runTest {
        val outcome = saveGroupEventNote("Parking B", analytics) {
            GenericFirestoreError(IllegalStateException("denied")).toFailure()
        }

        assertFalse(outcome.isSuccess)
        assertTrue(analytics.events.isEmpty())
    }
}
