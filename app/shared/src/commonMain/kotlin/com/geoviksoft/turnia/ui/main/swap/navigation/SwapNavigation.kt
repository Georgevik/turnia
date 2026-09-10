package com.geoviksoft.turnia.ui.main.swap.navigation

import androidx.navigation3.runtime.EntryProviderScope
import com.geoviksoft.turnia.navigation.main.routes.MainRoute
import com.geoviksoft.turnia.ui.main.swap.SwapScreen

fun EntryProviderScope<MainRoute>.swapNavigation() {
    entry<MainRoute.SwapTab> { SwapScreen() }
}
