package com.georgevik.turnia.navigation.root

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.georgevik.turnia.navigation.TurniaNavDisplay
import com.georgevik.turnia.navigation.root.routes.RootRoute
import com.georgevik.turnia.ui.main.MainScreen
import com.georgevik.turnia.ui.signin.SignInScreen
import com.georgevik.turnia.ui.splash.SplashScreen

@Composable
fun RootNavDisplay(
    snackbarHostState: SnackbarHostState,
    backStack: NavBackStack<NavKey>,
) {
    TurniaNavDisplay(
        backStack = backStack,
        entryProvider = entryProvider {
            entry<RootRoute.SplashKey> {
                SplashScreen(backStack = backStack, snackbar = snackbarHostState)
            }
            entry<RootRoute.SignInKey> { SignInScreen() }
            entry<RootRoute.MainKey> { MainScreen() }
        },
    )
}
