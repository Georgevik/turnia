package com.georgevik.turnia.core.data.datasource.firestore.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * One Firestore listener per key, however many collectors follow it.
 */
@OptIn(ExperimentalAtomicApi::class)
class SharedListeners<K, V>(
    private val scope: CoroutineScope,
    private val keepAlive: Duration = DEFAULT_KEEP_ALIVE,
) {

    // Immutable map swapped atomically: collectors arrive on whichever thread their flow runs on.
    private val listeners = AtomicReference(emptyMap<K, Flow<V>>())

    fun shared(key: K, listener: () -> Flow<V>): Flow<V> {
        while (true) {
            val current = listeners.load()
            current[key]?.let { return it }

            val shared = listener().shareIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(
                    stopTimeoutMillis = keepAlive.inWholeMilliseconds,
                    // Forgotten the moment the listener detaches: replaying what the document said
                    // in a previous session would answer "your cache is fine" without asking.
                    replayExpirationMillis = 0,
                ),
                replay = 1,
            )

            // Nothing is attached until somebody collects, so losing this race costs nothing.
            if (listeners.compareAndSet(current, current + (key to shared))) return shared
        }
    }

    companion object {
        val DEFAULT_KEEP_ALIVE: Duration = 5.seconds
    }
}
