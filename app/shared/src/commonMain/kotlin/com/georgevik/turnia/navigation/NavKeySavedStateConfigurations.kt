package com.georgevik.turnia.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

val rootRouteSavedStateConfiguration: SavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(RootRoute.SplashKey::class, RootRoute.SplashKey.serializer())
            subclass(RootRoute.SignInKey::class, RootRoute.SignInKey.serializer())
            subclass(RootRoute.MainKey::class, RootRoute.MainKey.serializer())
        }
    }
}

val mainRouteSavedStateConfiguration: SavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(MainRoute.CalendarTab::class, MainRoute.CalendarTab.serializer())
            subclass(MainRoute.GroupsTab::class, MainRoute.GroupsTab.serializer())
            subclass(MainRoute.ProfileTab::class, MainRoute.ProfileTab.serializer())
            subclass(MainRoute.ChangesTab::class, MainRoute.ChangesTab.serializer())
            subclass(MainRoute.EventMasterKey::class, MainRoute.EventMasterKey.serializer())
            subclass(MainRoute.EventTypeDetailKey::class, MainRoute.EventTypeDetailKey.serializer())
            subclass(MainRoute.GroupCalendarKey::class, MainRoute.GroupCalendarKey.serializer())
        }
    }
}
