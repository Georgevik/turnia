package com.georgevik.turnia.di

import com.georgevik.turnia.core.data.auth.AuthProvider
import org.koin.dsl.module

/**
 * Starts Koin from the iOS entry point (Swift).
 *
 * The Google Sign-In [AuthProvider] is implemented in Swift (GoogleSignIn is an
 * SPM/Swift SDK, not reachable from Kotlin/Native) and injected here.
 * Exposed as `KoinIOSKt.doInitKoin(authProvider:)` in the `Shared` framework.
 */
fun doInitKoin(authProvider: AuthProvider) {
    initKoin {
        modules(module { single<AuthProvider> { authProvider } })
    }
}
