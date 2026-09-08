package com.georgevik.turnia.navigation.main

import androidx.navigation3.runtime.NavKey
import com.georgevik.turnia.navigation.Navigator
import com.georgevik.turnia.navigation.main.routes.MainRoute

class MainNavigator(private val state: MainNavigationState) : Navigator {
    override fun goTo(route: NavKey) {
        val mainRoute = requireNotNull(route as? MainRoute) { "$route is not a MainRoute" }
        
        if (mainRoute in state.backStacks.keys) {
            state.topLevelRoute = mainRoute
        } else {
            state.backStacks.getValue(state.topLevelRoute).add(mainRoute)
        }
    }

    override fun replace(route: NavKey) {
        val mainRoute = requireNotNull(route as? MainRoute) { "$route is not a MainRoute" }
        val stack = state.backStacks.getValue(state.topLevelRoute)

        // The tab's own root is the floor of its stack: replacing it would leave the tab empty.
        if (stack.size > 1) stack.removeLastOrNull()
        stack.add(mainRoute)
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
