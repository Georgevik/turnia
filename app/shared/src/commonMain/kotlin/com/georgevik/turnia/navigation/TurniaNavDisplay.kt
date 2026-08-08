package com.georgevik.turnia.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.georgevik.turnia.ui.AboutScreen
import com.georgevik.turnia.ui.HomeScreen

/**
 * Root Navigation 3 host. Owns the back stack and maps each key to its screen.
 *
 * The view-model store decorator scopes a `ViewModelStore` to every back-stack
 * entry, so `koinViewModel` instances live and die with their destination
 * instead of being shared across the whole host.
 */
@Composable
fun TurniaNavDisplay() {
    val backStack = rememberNavBackStack(navKeySavedStateConfiguration, HomeKey)
    NavDisplay(
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<HomeKey> {
                HomeScreen(onOpenAbout = { backStack.add(AboutKey) })
            }
            entry<AboutKey> {
                AboutScreen(onBack = { backStack.removeLastOrNull() })
            }
        },
    )
}
