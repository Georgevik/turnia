package com.geoviksoft.turnia.di

import com.geoviksoft.turnia.core.di.coreModules
import com.mmk.kmpauth.core.KMPAuth
import com.mmk.kmpauth.google.google
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/**
 * Common startup: initializes KMPAuth (Google provider + Firebase backend, which
 * registers itself) and Koin.
 *
 * @param webClientId the Google OAuth **web** client id used as `serverId`.
 * @param config additional platform-specific Koin config (e.g. `androidContext(...)`).
 */
fun initKoin(webClientId: String, config: KoinAppDeclaration? = null): KoinApplication {
    KMPAuth.initialize {
        google(serverId = webClientId)
    }
    return startKoin {
        config?.invoke(this)
        modules(coreModules + featureModules)
    }
}
