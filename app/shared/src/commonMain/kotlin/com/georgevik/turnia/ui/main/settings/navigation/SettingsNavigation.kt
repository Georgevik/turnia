package com.georgevik.turnia.ui.main.settings.navigation

import androidx.navigation3.runtime.EntryProviderScope
import com.georgevik.turnia.navigation.main.routes.MainRoute
import com.georgevik.turnia.ui.main.settings.SettingsMenuScreen

fun EntryProviderScope<MainRoute>.settingsNavigation() {
    entry<MainRoute.SettingsMenuTab> { SettingsMenuScreen() }
}
