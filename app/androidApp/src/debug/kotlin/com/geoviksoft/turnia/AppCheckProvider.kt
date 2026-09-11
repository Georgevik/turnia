package com.geoviksoft.turnia

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Debug builds cannot pass Play Integrity (sideloaded, often on an emulator), so they attest with a
 * debug token instead. The SDK logs it on first launch, under the `DebugAppCheckProvider` tag; add it
 * to the console (App Check → Apps → Manage debug tokens) once per device.
 */
internal fun appCheckProviderFactory(): AppCheckProviderFactory =
    DebugAppCheckProviderFactory.getInstance()
