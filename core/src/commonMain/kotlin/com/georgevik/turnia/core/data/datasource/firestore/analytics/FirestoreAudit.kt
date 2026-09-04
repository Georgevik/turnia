package com.georgevik.turnia.core.data.datasource.firestore.analytics

import com.georgevik.turnia.core.data.logger.Logger
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

private const val TAG = "FirestoreAudit"

/** What one reporting class has spent so far. */
data class FirestoreUsage(
    val serverReads: Int = 0,
    val cachedReads: Int = 0,
    val writes: Int = 0,
) {
    operator fun plus(other: FirestoreUsage) = FirestoreUsage(
        serverReads = serverReads + other.serverReads,
        cachedReads = cachedReads + other.cachedReads,
        writes = writes + other.writes,
    )

    override fun toString(): String =
        "Read Server: $serverReads ReadCache: $cachedReads Writes: $writes"
}

/**
 * Counts what Firestore actually bills: one read per document the **server** returns, one write per
 * document sent to it. Documents answered from the local cache cost nothing and are counted apart,
 * so the caching work has a number to show for itself.
 *
 * Kept per reporting class, so the audit says who is spending and not only how much.
 */
@OptIn(ExperimentalAtomicApi::class)
object FirestoreAudit {


    private val summaryLogger = MutableStateFlow(Random.nextInt())

    init {
        GlobalScope.launch {
            summaryLogger.debounce(1.seconds).collect { printSummary() }
        }
    }

    // An immutable map swapped atomically: Firestore resolves its calls on its own threads, and a
    // plain map would both lose counts and break while the summary iterates it.
    private val usageByTag = AtomicReference(emptyMap<String, FirestoreUsage>())

    fun add(tag: String, usage: FirestoreUsage) {
        while (true) {
            val current = usageByTag.load()
            val updated = current + (tag to (current[tag] ?: FirestoreUsage()) + usage)
            if (usageByTag.compareAndSet(current, updated)) return
        }
    }

    fun triggerSummary() {
        summaryLogger.tryEmit(Random.nextInt())
    }

    /** Totals first, then a line per reporting class. */
    private fun printSummary() {
        val usage = usageByTag.load()
        val total = usage.values.fold(FirestoreUsage()) { acc, next -> acc + next }

        val summary = buildString {
            append(total)
            usage.entries.sortedBy { it.key }.forEach { (tag, tagUsage) ->
                append("\n    $tag -> $tagUsage")
            }
        }
        Logger.i(TAG, summary)
    }
}

fun QuerySnapshot.trackData(tag: String): QuerySnapshot = apply {
    val billed = documentChanges.size.takeIf { it > 0 } ?: 1
    trackRead(tag, billed, metadata.isFromCache)
}

fun DocumentSnapshot.trackData(tag: String): DocumentSnapshot = apply {
    // A document that does not exist still costs a read.
    trackRead(tag, 1, metadata.isFromCache)
}

fun interface PendingWrite {
    fun committed()
}

/** A write is never served from a cache: it is billed even while the device is offline. */
fun trackWrite(tag: String, documents: Int = 1) {
    FirestoreAudit.add(tag, FirestoreUsage(writes = documents))
    FirestoreAudit.triggerSummary()
}

private fun trackRead(tag: String, documents: Int, fromCache: Boolean) {
    if (fromCache) {
        FirestoreAudit.add(tag, FirestoreUsage(cachedReads = documents))
        return
    }

    FirestoreAudit.add(tag, FirestoreUsage(serverReads = documents))
    Logger.d(TAG, "$tag - SERVER READ: $documents")
    FirestoreAudit.triggerSummary()
}
