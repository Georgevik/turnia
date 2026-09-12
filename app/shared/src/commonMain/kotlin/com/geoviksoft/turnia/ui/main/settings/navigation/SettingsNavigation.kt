package com.geoviksoft.turnia.ui.main.settings.navigation

import androidx.navigation3.runtime.EntryProviderScope
import com.geoviksoft.turnia.navigation.main.routes.MainRoute
import com.geoviksoft.turnia.ui.main.settings.SettingsMenuScreen
import com.geoviksoft.turnia.ui.main.settings.mygroups.MyGroupsScreen

fun EntryProviderScope<MainRoute>.settingsNavigation() {
    entry<MainRoute.SettingsMenuTab> { SettingsMenuScreen() }
    entry<MainRoute.MyGroups> { MyGroupsScreen() }
}
