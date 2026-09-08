package com.georgevik.turnia.ui.main.groups.navigation

import androidx.navigation3.runtime.EntryProviderScope
import com.georgevik.turnia.navigation.main.routes.MainRoute
import com.georgevik.turnia.ui.main.groups.GroupsScreen
import com.georgevik.turnia.ui.main.groups.admin.AdminGroupsScreen

/** "Grupos" tab, and the admin list reached from Ajustes. */
fun EntryProviderScope<MainRoute>.groupsNavigation() {
    entry<MainRoute.GroupsTab> { GroupsScreen() }
    entry<MainRoute.AdminGroups> { AdminGroupsScreen() }
}
