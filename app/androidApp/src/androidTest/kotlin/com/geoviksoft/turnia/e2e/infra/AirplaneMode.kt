package com.geoviksoft.turnia.e2e.infra

import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import java.net.InetSocketAddress
import java.net.Socket

/**
 * The whole device offline, emulators included: `Network` only cuts Firestore, and a Storage
 * upload or a callable goes over plain HTTP. Always switched back, so a failing test does not leave
 * the emulator offline for the next one.
 */
internal object AirplaneMode {

    fun <T> on(block: () -> T): T {
        shell("cmd connectivity airplane-mode enable")
        try {
            return block()
        } finally {
            shell("cmd connectivity airplane-mode disable")
            awaitEmulators()
        }
    }

    /** The radios take a few seconds to come back; until they do, the emulators are unreachable. */
    private fun awaitEmulators() {
        val deadline = SystemClock.uptimeMillis() + RECONNECT_TIMEOUT_MS
        while (!reachable()) {
            check(SystemClock.uptimeMillis() < deadline) { "The emulators are still unreachable after airplane mode" }
            SystemClock.sleep(250)
        }
    }

    private fun reachable(): Boolean = runCatching {
        Socket().use { it.connect(InetSocketAddress(Emulator.HOST, Emulator.FIRESTORE_PORT), 1_000) }
    }.isSuccess

    private const val RECONNECT_TIMEOUT_MS = 30_000L

    private fun shell(command: String) {
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command).close()
    }
}
