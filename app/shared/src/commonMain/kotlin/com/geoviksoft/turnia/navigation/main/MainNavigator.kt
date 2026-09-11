package com.geoviksoft.turnia.navigation.main

import androidx.navigation3.runtime.NavKey
import com.geoviksoft.turnia.navigation.Navigator
import com.geoviksoft.turnia.navigation.main.routes.MainRoute

class MainNavigator(private val state: MainNavigationState) : Navigator {
    override fun goTo(route: NavKey) {
        val mainRoute = requireNotNull(route as? MainRoute) { "$route is not a MainRoute" }
        
        if (mainRoute in state.backStacks.keys) {
            state.topLevelRoute = mainRoute
        } else {
            state.backStacks.getValue(state.topLevelRoute).add(mainRoute)
        }
    }

    /** Switches to [tab] showing its own screen, not whatever was left stacked on top of it. */
    fun goToRoot(tab: MainRoute) {
        val stack = state.backStacks.getValue(tab)
        while (stack.size > 1) stack.removeLastOrNull()
        state.topLevelRoute = tab
    }

    override fun goBack() {
        val currentStack = state.backStacks.getValue(state.topLevelRoute)
        if (currentStack.last() == state.topLevelRoute) {
            state.topLevelRoute = state.startRoute
        } else {
            currentStack.removeLastOrNull()
        }
    }

    override fun popToRoot() {
        state.backStacks.values.forEach { stack ->
            while (stack.size > 1) stack.removeLastOrNull()
        }
    }
}
