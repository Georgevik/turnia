package com.georgevik.turnia.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.georgevik.turnia.ui.AboutScreen
import com.georgevik.turnia.ui.signin.SignInScreen
import com.georgevik.turnia.ui.splash.SplashScreen
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * Root Navigation 3 host. Owns the back stack and maps each key to its screen.
 *
 * The view-model store decorator scopes a `ViewModelStore` to every back-stack
 * entry, so `koinViewModel` instances live and die with their destination
 * instead of being shared across the whole host.
 */
@Composable
fun TurniaNavDisplay(snackbarHostState: SnackbarHostState, backStack : NavBackStack<NavKey>) {
    NavDisplay(
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Route.SpashKey> {
                SplashScreen(snackbar = snackbarHostState, onNextScreen = { key ->
                    backStack.clear()
                    backStack.add(key)
                })
            }
            entry<Route.SignInKey> {
                SignInScreen(onOpenAbout = { backStack.add(Route.AboutKey) })
            }
            entry<Route.AboutKey> {
                AboutScreen(onBack = { backStack.removeLastOrNull() })
            }
        },
    )
}

/**
 * Saved-state configuration that teaches the back-stack serializer how to
 * persist each [NavKey] subtype. Required on non-JVM targets (e.g. iOS), where
 * reflection-based polymorphism is unavailable, so every key is registered
 * explicitly here.
 */
val navKeySavedStateConfiguration: SavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Route.SignInKey::class, Route.SignInKey.serializer())
            subclass(Route.AboutKey::class, Route.AboutKey.serializer())
            subclass(Route.SpashKey::class, Route.SpashKey.serializer())
        }
    }
}
