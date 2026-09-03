package com.georgevik.turnia.core.system

import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.Timestamp
import kotlin.time.Instant

/**
 * `null` while the write that set it is still pending: Firestore resolves a server timestamp only
 * once it reaches the server, and reports it as unset until then.
 */
fun BaseTimestamp?.toInstantOrNull(): Instant? = when (this) {
    is Timestamp -> Instant.fromEpochSeconds(seconds, nanoseconds)
    else -> null
}
