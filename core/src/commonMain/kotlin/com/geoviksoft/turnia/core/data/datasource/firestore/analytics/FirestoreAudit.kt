package com.geoviksoft.turnia.core.data.datasource.firestore.analytics

import com.geoviksoft.turnia.core.data.logger.Logger
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
 * Kept per reporting class **and per call within it**: knowing that a datasource spent forty reads
 * says nothing about which of its five queries did, and that is the only thing you can act on.
 * Functions are the exception, keyed by the callable alone: there the call *is* the spender.
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
    // plain map would both lose counts and break while the summary iterates it. Nested by class and
    // then by the call inside it.
    private val usageByTag = AtomicReference(emptyMap<String, Map<String, FirestoreUsage>>())

    fun add(tag: String, operation: String, usage: FirestoreUsage) {
        while (true) {
            val current = usageByTag.load()
            val operations = current[tag] ?: emptyMap()
            val merged = (operations[operation] ?: FirestoreUsage()) + usage
            val updated = current + (tag to (operations + (operation to merged)))
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
     *
     * Under Firestore each class breaks down into the calls that spent the money. Functions do not:
     * the callable's name is already the whole answer.
     */
    private fun printSummary() {
        val usage = usageByTag.load()
        val byTag = usage.mapValues { (_, operations) -> operations.values.total() }
        val total = byTag.values.total()
        val sorted = byTag.entries.sortedBy { it.key }

        val summary = buildString {
            append("Firestore  ${total.firestoreLine()}")
            sorted.filter { it.value.touchesFirestore }.forEach { (tag, tagUsage) ->
                append("\n    $tag -> ${tagUsage.firestoreLine()}")

                usage.getValue(tag).entries
                    .filter { it.value.touchesFirestore }
                    .sortedByDescending { it.value.serverReads }
                    .forEach { (operation, callUsage) ->
                        append("\n        · $operation -> ${callUsage.firestoreLine()}")
                    }
            }

            append("\nFunctions  ${total.callsLine()}")
            sorted.filter { it.value.calls > 0 }.forEach { (tag, tagUsage) ->
                append("\n    $tag -> ${tagUsage.callsLine()}")
            }
        }
        Logger.i(TAG, summary)
    }

    private fun Iterable<FirestoreUsage>.total() =
        fold(FirestoreUsage()) { acc, next -> acc + next }
}

/**
 * @param operation which query or snapshot this is, within [tag]. Short and stable — it is a key,
 *   so a name built from a group id would make every group its own line.
 */
fun QuerySnapshot.trackData(tag: String, operation: String): QuerySnapshot = apply {
    val billed = documentChanges.size.takeIf { it > 0 } ?: 1
    trackRead(tag, operation, billed, metadata.isFromCache)
}

fun DocumentSnapshot.trackData(tag: String, operation: String): DocumentSnapshot = apply {
    // A document that does not exist still costs a read.
    trackRead(tag, operation, 1, metadata.isFromCache)
}

fun interface PendingWrite {
    fun committed()
}

/**
 * A callable Cloud Function invocation, counted under the callable's own name: what costs money is
 * the function, and one datasource calls several.
 */
fun trackFunction(name: String) {
    FirestoreAudit.add(name, name, FirestoreUsage(calls = 1))
    Logger.w(TAG, "$name - FUNCTION CALL")
    FirestoreAudit.triggerSummary()
}

/** A write is never served from a cache: it is billed even while the device is offline. */
fun trackWrite(tag: String, operation: String, documents: Int = 1) {
    FirestoreAudit.add(tag, operation, FirestoreUsage(writes = documents))
    FirestoreAudit.triggerSummary()
}

private fun trackRead(tag: String, operation: String, documents: Int, fromCache: Boolean) {
    if (fromCache) {
        FirestoreAudit.add(tag, operation, FirestoreUsage(cachedReads = documents))
        return
    }

    FirestoreAudit.add(tag, operation, FirestoreUsage(serverReads = documents))
    Logger.w(TAG, "$tag.$operation - SERVER READ: $documents")
    FirestoreAudit.triggerSummary()
}
