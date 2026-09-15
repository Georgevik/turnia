package com.geoviksoft.turnia.di

import com.geoviksoft.turnia.core.di.coreModules
import com.geoviksoft.turnia.core.system.BuildInfo
import com.geoviksoft.turnia.demo.demoModule
import com.mmk.kmpauth.core.KMPAuth
import com.mmk.kmpauth.google.google
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * Common startup: initializes KMPAuth (Google provider + Firebase backend, which
 * registers itself) and Koin.
 *
 * @param webClientId the Google OAuth **web** client id used as `serverId`.
 * @param buildInfo what kind of binary the platform built.
 * @param demo runs the app on made-up data instead of Firebase. Debug builds only: each platform
 *   decides, and a release build never passes true.
 * @param config additional platform-specific Koin config (e.g. `androidContext(...)`).
 */
fun initKoin(
    webClientId: String,
    buildInfo: BuildInfo,
    demo: Boolean = false,
    config: KoinAppDeclaration? = null,
): KoinApplication {
    KMPAuth.initialize {
        google(serverId = webClientId)
    }
    return startKoin {
        config?.invoke(this)
        modules(module { single { buildInfo } })
        modules(coreModules + featureModules)
        // Last, so its bindings replace the real ones.
        if (demo) modules(demoModule)
    }
}
