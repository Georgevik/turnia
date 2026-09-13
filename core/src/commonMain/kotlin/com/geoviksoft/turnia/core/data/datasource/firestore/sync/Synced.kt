package com.geoviksoft.turnia.core.data.datasource.firestore.sync

import dev.gitlive.firebase.firestore.SnapshotMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** A listener's value, and whether the server stands behind it yet. */
data class Synced<T>(val value: T, val confirmed: Boolean)

/** Neither answered from the cache nor carrying a write of this device the server has not taken. */
val SnapshotMetadata.isConfirmed: Boolean get() = !isFromCache && !hasPendingWrites

/**
 * The listener's value as soon as the server has confirmed it — immediately when a listener that is
 * already attached has — or the cached value when no confirmation arrives within [timeout], which
 * is what being offline looks like.
 *
 * A one-shot `get()` next to a live listener on the same document is a second bill for an answer
 * the listener is about to give for free: a re-attach inside 30 minutes costs nothing, a `get()`
 * always costs a read.
 */
suspend fun <T> Flow<Synced<T>>.awaitConfirmed(timeout: Duration = CONFIRMATION_TIMEOUT): T {
    val synced = withTimeoutOrNull(timeout) { first { it.confirmed } }
        ?: withTimeoutOrNull(timeout) { first() }
        ?: error("The listener produced no value within $timeout")

    return synced.value
}

val CONFIRMATION_TIMEOUT: Duration = 3.seconds
