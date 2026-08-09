package com.georgevik.turnia.di

import com.georgevik.turnia.core.di.coreModule
import com.georgevik.turnia.core.data.logger.Logger
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/**
 * Common entry point to start Koin on every platform.
 *
 * @param config additional platform-specific configuration
 *   (e.g. `androidContext(...)` on Android).
 */
fun initKoin(config: KoinAppDeclaration? = null): KoinApplication {
    Logger.i( TAG, "Starting Koin")
    return startKoin {
        Logger.i( TAG, "Invoking platform dependencies")
        config?.invoke(this)
        Logger.i( TAG, "Invoking shared dependencies")
        modules(coreModule, appModule)
        Logger.i( TAG, "Finish")
    }
}

private const val TAG = "Koin"
