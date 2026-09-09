package com.geoviksoft.turnia.di

/**
 * Starts KMPAuth + Koin from the iOS entry point (Swift).
 * Exposed as `KoinIOSKt.doInitKoin(webClientId:)` in the `Shared` framework.
 *
 * @param webClientId the Google OAuth **web** client id used as `serverId`.
 */
fun doInitKoin(webClientId: String) {
    initKoin(webClientId)
}
