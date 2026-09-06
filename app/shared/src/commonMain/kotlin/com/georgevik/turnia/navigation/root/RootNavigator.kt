package com.georgevik.turnia.navigation.root

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.georgevik.turnia.navigation.Navigator

/**
 * Navigates the root back stack — the one that hosts Main and the full-screen destinations layered
 * over it. Its last entry is never popped: Main (or the splash/sign-in before it) is the floor.
 */
class RootNavigator(private val backStack: NavBackStack<NavKey>) : Navigator {
    override fun goTo(route: NavKey) {
        backStack.add(route)
    }

    override fun goBack() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }

    override fun popToRoot() {
        while (backStack.size > 1) backStack.removeLastOrNull()
    }
}
