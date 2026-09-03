package com.georgevik.turnia.core.domain.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Announces the end of a session so every in-memory cache can drop what it holds. Those caches are
 * keyed by nothing but the app being open, so without this the next person to sign in on the device
 * reads the previous one's data.
 *
 * A [SharedFlow] and not the session state: this is the *moment* of signing out, not a condition to
 * observe. A cache subscribing later must not wipe itself just because nobody happens to be signed
 * in yet, and a replayed value would do exactly that.
 */
class SessionEvents {

    private val _signedOut = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val signedOut: SharedFlow<Unit> = _signedOut.asSharedFlow()

    fun notifySignedOut() {
        // Nobody listening means nothing to clear: with no replay the signal is simply dropped.
        _signedOut.tryEmit(Unit)
    }
}

/** Registers [clear] to run on every sign-out, for the lifetime of [scope]. */
fun SessionEvents.clearOnSignOut(scope: CoroutineScope, clear: () -> Unit) {
    scope.launch { signedOut.collect { clear() } }
}
