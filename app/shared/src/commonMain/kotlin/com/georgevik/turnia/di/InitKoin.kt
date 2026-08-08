package com.georgevik.turnia.di

import com.georgevik.turnia.core.di.coreModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/**
 * Common entry point to start Koin on every platform.
 *
 * @param config additional platform-specific configuration
 *   (e.g. `androidContext(...)` on Android).
 */
fun initKoin(config: KoinAppDeclaration? = null): KoinApplication =
    startKoin {
        config?.invoke(this)
        modules(coreModule, appModule)
    }
