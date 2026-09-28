package com.geoviksoft.turnia.core.data.config

import com.geoviksoft.turnia.core.domain.model.EventKind
import com.geoviksoft.turnia.core.fakes.FakeAppConfigRepository
import com.geoviksoft.turnia.core.fakes.InMemoryDataStore
import com.geoviksoft.turnia.core.fakes.RecordingAnalytics
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SharePromptRepositoryImplTest {

    private val analytics = RecordingAnalytics()
    private val repository = SharePromptRepositoryImpl(InMemoryDataStore(), FakeAppConfigRepository(), analytics)

    @Test
    fun theFirstShiftReportsTyped() = runTest {
        repository.eventAdded(EventKind.Typed)

        val first = analytics.named("first_event_added").single()
        assertEquals("typed", first.parameters["kind"])
    }

    @Test
    fun theFirstOneOffReportsOneOff() = runTest {
        repository.eventAdded(EventKind.OneOff)

        assertEquals("one_off", analytics.named("first_event_added").single().parameters["kind"])
    }

    @Test
    fun onlyTheFirstEventReports() = runTest {
        repository.eventAdded(EventKind.OneOff)
        repository.eventAdded(EventKind.Typed)

        assertEquals(1, analytics.named("first_event_added").size)
    }
}
