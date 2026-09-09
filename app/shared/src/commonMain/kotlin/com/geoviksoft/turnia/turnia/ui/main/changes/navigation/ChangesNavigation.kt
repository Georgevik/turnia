package com.geoviksoft.turnia.ui.main.changes.navigation

import androidx.navigation3.runtime.EntryProviderScope
import com.geoviksoft.turnia.navigation.main.routes.MainRoute
import com.geoviksoft.turnia.ui.main.changes.ChangesScreen

fun EntryProviderScope<MainRoute>.changesNavigation() {
    entry<MainRoute.ChangesTab> { ChangesScreen() }
}
