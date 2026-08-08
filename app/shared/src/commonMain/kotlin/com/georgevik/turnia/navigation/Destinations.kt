package com.georgevik.turnia.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * Navigation 3 back-stack keys. Each key identifies a destination and is
 * `@Serializable` so the back stack survives configuration changes and
 * process death via `rememberNavBackStack`.
 */
@Serializable
data object HomeKey : NavKey

@Serializable
data object AboutKey : NavKey

/**
 * Saved-state configuration that teaches the back-stack serializer how to
 * persist each [NavKey] subtype. Required on non-JVM targets (e.g. iOS), where
 * reflection-based polymorphism is unavailable, so every key is registered
 * explicitly here.
 */
val navKeySavedStateConfiguration: SavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(HomeKey::class, HomeKey.serializer())
            subclass(AboutKey::class, AboutKey.serializer())
        }
    }
}
