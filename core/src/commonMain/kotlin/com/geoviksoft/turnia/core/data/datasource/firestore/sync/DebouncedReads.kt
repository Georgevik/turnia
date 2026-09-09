package com.geoviksoft.turnia.core.data.datasource.firestore.sync

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * Serves the last answer to a question for [window] before asking again.
 *
 * Every datasource asks a sync document whether its cache is behind, so opening a calendar can ask
 * several times over; the window bounds how stale a "your cache is fine" verdict can be, in exchange
 * for one read instead of one per question.
 */
@OptIn(ExperimentalAtomicApi::class)
class DebouncedReads<K, V>(private val window: Duration) {

    private data class Entry<V>(val value: V, val readAt: Instant)

    // Immutable map swapped atomically: Firestore resolves its calls on its own threads, and a
    // plain map would both lose entries and break while another thread reads it.
    private val entries = AtomicReference(emptyMap<K, Entry<V>>())

    fun cached(key: K): V? = entries.load()[key]
        ?.takeIf { Clock.System.now() - it.readAt < window }
        ?.value

    fun remember(key: K, value: V) = update { it + (key to Entry(value, Clock.System.now())) }

    /** After a write the document is ours and different: the next question deserves a real read. */
    fun forget(key: K) = update { it - key }

    private fun update(block: (Map<K, Entry<V>>) -> Map<K, Entry<V>>) {
        while (true) {
            val current = entries.load()
            if (entries.compareAndSet(current, block(current))) return
        }
    }
}
