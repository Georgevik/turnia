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
    /** Callable Cloud Functions invoked. Billed per invocation, not per document. */
    val calls: Int = 0,
) {
    operator fun plus(other: FirestoreUsage) = FirestoreUsage(
        serverReads = serverReads + other.serverReads,
        cachedReads = cachedReads + other.cachedReads,
        writes = writes + other.writes,
        calls = calls + other.calls,
    )

    /** Documents, which is what Firestore prices. */
    val touchesFirestore: Boolean get() = serverReads > 0 || cachedReads > 0 || writes > 0

    fun firestoreLine(): String =
        "Read Server: $serverReads ReadCache: $cachedReads Writes: $writes"

    fun callsLine(): String = "Calls: $calls"
}

/**
 * Counts what Firebase actually bills: one read per document the **server** returns, one write per
 * document sent to it, and one invocation per callable Cloud Function. Documents answered from the
 * local cache cost nothing and are counted apart, so the caching work has a number to show for
 * itself.
 *
 * A function's own reads and writes are **not** in here. They happen on Google's servers with admin
 * privileges and the client never sees them, so one call in this audit can hide a dozen documents.
 *
 * Kept per reporting class, and per callable for the functions: there the spender is the function.
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

    /**
     * Two blocks, because they are two different bills and two different units: Firestore charges
     * per document, a callable charges per invocation. Mixing them on one line meant every
     * datasource reporting `Calls: 0` and every function reporting three zeroes.
     *
     * Totals first in each, then a line per key — a reporting class under Firestore, a callable's
     * name under Functions — and only those that spent something in that block.
     */
    private fun printSummary() {
        val usage = usageByTag.load()
        val total = usage.values.fold(FirestoreUsage()) { acc, next -> acc + next }
        val byTag = usage.entries.sortedBy { it.key }

        val summary = buildString {
            append("Firestore  ${total.firestoreLine()}")
            byTag.filter { it.value.touchesFirestore }.forEach { (tag, tagUsage) ->
                append("\n    $tag -> ${tagUsage.firestoreLine()}")
            }

            append("\nFunctions  ${total.callsLine()}")
            byTag.filter { it.value.calls > 0 }.forEach { (tag, tagUsage) ->
                append("\n    $tag -> ${tagUsage.callsLine()}")
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

/**
 * A callable Cloud Function invocation, counted under the callable's own name: what costs money is
 * the function, and one datasource calls several.
 */
fun trackFunction(name: String) {
    FirestoreAudit.add(name, FirestoreUsage(calls = 1))
    Logger.w(TAG, "$name - FUNCTION CALL")
    FirestoreAudit.triggerSummary()
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
    Logger.w(TAG, "$tag - SERVER READ: $documents")
    FirestoreAudit.triggerSummary()
}
