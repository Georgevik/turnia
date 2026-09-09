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
            subclass(RootRoute.SignInKey::class, RootRoute.SignInKey.serializer())
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
            subclass(RootRoute.NotificationsKey::class, RootRoute.NotificationsKey.serializer())
        }
    }
}
