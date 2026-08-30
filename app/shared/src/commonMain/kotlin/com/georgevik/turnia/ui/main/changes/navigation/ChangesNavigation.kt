package com.georgevik.turnia.ui.main.changes.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.georgevik.turnia.navigation.main.routes.MainRoute
import com.georgevik.turnia.ui.main.changes.ChangesScreen

fun EntryProviderScope<NavKey>.changesNavigation() {
    entry<MainRoute.ChangesTab> { ChangesScreen() }
}
