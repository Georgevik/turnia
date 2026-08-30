package com.georgevik.turnia.navigation.main

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import com.georgevik.turnia.navigation.main.routes.MainRoute
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

internal val mainRouteSavedStateConfiguration: SavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(MainRoute.CalendarTab::class, MainRoute.CalendarTab.serializer())
            subclass(MainRoute.GroupsTab::class, MainRoute.GroupsTab.serializer())
            subclass(MainRoute.ProfileTab::class, MainRoute.ProfileTab.serializer())
            subclass(MainRoute.ChangesTab::class, MainRoute.ChangesTab.serializer())
            subclass(MainRoute.EventMasterKey::class, MainRoute.EventMasterKey.serializer())
            subclass(MainRoute.EventTypeDetailKey::class, MainRoute.EventTypeDetailKey.serializer())
            subclass(MainRoute.GroupCalendar::class, MainRoute.GroupCalendar.serializer())
        }
    }
}
