package com.geoviksoft.turnia.e2e.infra

import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import org.koin.core.context.GlobalContext

/**
 * The app's Firestore connection, cut and restored: what airplane mode looks like to it. The local
 * cache keeps answering reads and writes keep queueing, and nothing reaches the emulator until the
 * connection is back.
 *
 * Resolved from Koin rather than through `Firebase.firestore`, so it is provably the instance the
 * app itself reads and writes through. The emulators stay up, which is what separates being offline
 * from having no backend at all.
 */
internal object Network {

    val firestore: FirebaseFirestore get() = GlobalContext.get().get()

    fun goOffline() = runBlocking { firestore.disableNetwork() }

    fun goOnline() = runBlocking { firestore.enableNetwork() }

    /** Runs [block] with the connection cut, restoring it even if [block] fails. */
    fun <T> offline(block: () -> T): T {
        goOffline()
        try {
            return block()
        } finally {
            goOnline()
        }
    }
}
