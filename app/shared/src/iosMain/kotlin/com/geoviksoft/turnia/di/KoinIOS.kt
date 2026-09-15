package com.geoviksoft.turnia.di

import com.geoviksoft.turnia.core.system.BuildInfo

/**
 * Starts KMPAuth + Koin from the iOS entry point (Swift).
 * Exposed as `KoinIOSKt.doInitKoin(webClientId:isDebug:demo:)` in the `Shared` framework.
 *
 * @param webClientId the Google OAuth **web** client id used as `serverId`.
 * @param isDebug whether Swift was compiled with `DEBUG`.
 * @param demo runs on made-up data; Swift only passes true from a debug build.
 */
fun doInitKoin(webClientId: String, isDebug: Boolean, demo: Boolean) {
    initKoin(webClientId, BuildInfo(isDebug), demo)
}
