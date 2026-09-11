package com.geoviksoft.turnia.ui.main.groups.navigation

import androidx.navigation3.runtime.EntryProviderScope
import com.geoviksoft.turnia.navigation.main.routes.MainRoute
import com.geoviksoft.turnia.ui.main.groups.GroupsScreen

/** "Grupos" tab. */
fun EntryProviderScope<MainRoute>.groupsNavigation() {
    entry<MainRoute.GroupsTab> { GroupsScreen() }
}
