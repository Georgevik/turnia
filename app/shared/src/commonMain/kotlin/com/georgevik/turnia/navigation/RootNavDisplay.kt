package com.georgevik.turnia.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.georgevik.turnia.ui.main.MainScreen
import com.georgevik.turnia.ui.signin.SignInScreen
import com.georgevik.turnia.ui.splash.SplashScreen

@Composable
fun RootNavDisplay(
    snackbarHostState: SnackbarHostState,
    backStack: NavBackStack<NavKey>,
    onSplashMinimumDurationElapsed: () -> Unit,
) {
    NavDisplay(
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<RootRoute.SplashKey> {
                SplashScreen(snackbar = snackbarHostState, onMinimumDurationElapsed = onSplashMinimumDurationElapsed)
            }
            entry<RootRoute.SignInKey> { SignInScreen() }
            entry<RootRoute.MainKey> { MainScreen() }
        },
    )
}
