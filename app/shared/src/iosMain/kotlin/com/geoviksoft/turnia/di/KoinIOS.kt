package com.geoviksoft.turnia.di

/**
 * Starts KMPAuth + Koin from the iOS entry point (Swift).
 * Exposed as `KoinIOSKt.doInitKoin(webClientId:)` in the `Shared` framework.
 *
 * @param webClientId the Google OAuth **web** client id used as `serverId`.
 * @param demo runs on made-up data; Swift only passes true from a debug build.
 */
fun doInitKoin(webClientId: String, demo: Boolean) {
    initKoin(webClientId, demo)
}
