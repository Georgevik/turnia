package com.georgevik.turnia.navigation.main

import androidx.navigation3.runtime.NavKey
import com.georgevik.turnia.navigation.Navigator

class MainNavigator(private val state: MainNavigationState) : Navigator {
    override fun goTo(route: NavKey) {
        if (route in state.backStacks.keys) {
            state.topLevelRoute = route
        } else {
            state.backStacks.getValue(state.topLevelRoute).add(route)
        }
    }

    override fun goBack() {
        val currentStack = state.backStacks.getValue(state.topLevelRoute)
        if (currentStack.last() == state.topLevelRoute) {
            state.topLevelRoute = state.startRoute
        } else {
            currentStack.removeLastOrNull()
        }
    }
}
