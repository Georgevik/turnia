package com.geoviksoft.turnia.core.data.datasource.firestore

import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackData
import com.geoviksoft.turnia.core.data.logger.Logger
import dev.gitlive.firebase.firestore.DocumentReference
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.Source
import kotlin.coroutines.cancellation.CancellationException

/**
 * The document as the cache holds it, or null when the cache does not have it.
 *
 * Unlike a collection query, a document read with [Source.CACHE] fails rather than coming back
 * empty when the document was never cached, so every cache-first read goes through here.
 */
suspend fun DocumentReference.getCached(tag: String, operation: String): DocumentSnapshot? = try {
    get(Source.CACHE).trackData(tag, operation).takeIf { it.exists }
} catch (cancellation: CancellationException) {
    // Not a cache miss: treating it as one sends a cancelled caller on to a server read.
    throw cancellation
} catch (exception: Exception) {
    Logger.d(tag, "Not cached yet: ${exception.message}")
    null
}
