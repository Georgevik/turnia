package com.georgevik.turnia.ui.main.changes.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.georgevik.turnia.navigation.MainRoute
import com.georgevik.turnia.ui.main.changes.ChangesScreen

/** Changes tab (feature-flag gated, see `MainScreen.kt`). */
fun EntryProviderScope<NavKey>.changesNavigation() {
    entry<MainRoute.ChangesTab> { ChangesScreen() }
}
