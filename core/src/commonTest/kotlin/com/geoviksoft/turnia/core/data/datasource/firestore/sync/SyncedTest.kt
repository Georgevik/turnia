package com.geoviksoft.turnia.core.data.datasource.firestore.sync

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds

class SyncedTest {

    @Test
    fun waitsPastTheCacheForTheServerConfirmation() = runTest {
        val listener = flow {
            emit(Synced("cached", confirmed = false))
            delay(500)
            emit(Synced("server", confirmed = true))
        }

        assertEquals("server", listener.awaitConfirmed(timeout = 3.seconds))
        assertEquals(500, currentTime)
    }

    @Test
    fun answersAtOnceWhenTheReplayIsAlreadyConfirmed() = runTest {
        val listener = MutableSharedFlow<Synced<String>>(replay = 1)
        listener.emit(Synced("server", confirmed = true))

        assertEquals("server", listener.awaitConfirmed(timeout = 3.seconds))
        assertEquals(0, currentTime)
    }

    @Test
    fun fallsBackToTheCachedValueWhenOffline() = runTest {
        val listener = MutableSharedFlow<Synced<String>>(replay = 1)
        listener.emit(Synced("cached", confirmed = false))

        assertEquals("cached", listener.awaitConfirmed(timeout = 3.seconds))
        assertEquals(3_000, currentTime)
    }

    @Test
    fun failsWhenTheListenerNeverAnswers() = runTest {
        assertFailsWith<NoSuchElementException> { emptyFlow<Synced<String>>().awaitConfirmed() }
    }
}
