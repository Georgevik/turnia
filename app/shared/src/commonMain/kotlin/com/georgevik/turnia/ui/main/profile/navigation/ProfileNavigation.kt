package com.georgevik.turnia.ui.main.profile.navigation

import androidx.navigation3.runtime.EntryProviderScope
import com.georgevik.turnia.navigation.main.routes.MainRoute
import com.georgevik.turnia.ui.main.profile.ProfileScreen

fun EntryProviderScope<MainRoute>.profileNavigation() {
    entry<MainRoute.ProfileTab> { ProfileScreen() }
}
