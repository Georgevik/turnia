package com.geoviksoft.turnia.navigation.root

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import com.geoviksoft.turnia.navigation.root.routes.RootRoute
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

internal val rootRouteSavedStateConfiguration: SavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(RootRoute.SplashKey::class, RootRoute.SplashKey.serializer())
            subclass(RootRoute.OnboardingKey::class, RootRoute.OnboardingKey.serializer())
            subclass(RootRoute.SignInKey::class, RootRoute.SignInKey.serializer())
            subclass(RootRoute.CreateAccountKey::class, RootRoute.CreateAccountKey.serializer())
            subclass(RootRoute.MainKey::class, RootRoute.MainKey.serializer())
            subclass(
                RootRoute.EventTypeDetailKey::class,
                RootRoute.EventTypeDetailKey.serializer(),
            )
            subclass(
                RootRoute.PersonalEventTypesKey::class,
                RootRoute.PersonalEventTypesKey.serializer(),
            )
            subclass(RootRoute.MyProfileKey::class, RootRoute.MyProfileKey.serializer())
            subclass(RootRoute.PreferencesKey::class, RootRoute.PreferencesKey.serializer())
            subclass(RootRoute.AboutKey::class, RootRoute.AboutKey.serializer())
            subclass(RootRoute.ShiftSetupKey::class, RootRoute.ShiftSetupKey.serializer())
        }
    }
}
