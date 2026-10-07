package com.geoviksoft.turnia

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Debug builds cannot pass Play Integrity (sideloaded, often on an emulator), so they attest with a
 * debug token instead: the one [DebugAppCheckSecretRegistrar] takes from `local.properties`, or one
 * the SDK makes up and logs under the `DebugAppCheckProvider` tag. Either way it is only accepted
 * once it is in the console (App Check → Apps → Manage debug tokens).
 */
internal fun appCheckProviderFactory(): AppCheckProviderFactory =
    DebugAppCheckProviderFactory.getInstance()
