package com.georgevik.turnia.ui.main.profile.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.georgevik.turnia.navigation.MainRoute
import com.georgevik.turnia.ui.main.profile.ProfileScreen

/** Profile tab. Future sub-screens (Groups, shared calendars, subscription, settings) get
 * their own [MainRoute] entries here, reached via `LocalNavigator.current.goTo(...)`. */
fun EntryProviderScope<NavKey>.profileNavigation() {
    entry<MainRoute.ProfileTab> { ProfileScreen() }
}
