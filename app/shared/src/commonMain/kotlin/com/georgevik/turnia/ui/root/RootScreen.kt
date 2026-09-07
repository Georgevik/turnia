package com.georgevik.turnia.ui.root

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.georgevik.turnia.core.domain.model.UserSession
import com.georgevik.turnia.navigation.LocalNavigator
import com.georgevik.turnia.navigation.LocalRootNavigator
import com.georgevik.turnia.navigation.root.RootNavDisplay
import com.georgevik.turnia.navigation.root.RootNavigator
import com.georgevik.turnia.navigation.root.rootRouteSavedStateConfiguration
import com.georgevik.turnia.navigation.root.routes.RootRoute
import com.georgevik.turnia.ui.system.LocalSnackbar
import com.georgevik.turnia.ui.system.RequestNotificationPermission
import com.georgevik.turnia.ui.system.TurniaSnackbarHost
import com.georgevik.turnia.ui.system.TurniaTheme
import org.koin.compose.viewmodel.koinViewModel

/**
 * Hosts the root back stack. The splash owns the initial hand-off (Main vs. SignIn) once its
 * minimum duration elapses and the session resolves; this effect then keeps the app in sync with
 * later [UserSession] changes — routing to Main on sign-in and back to SignIn on sign-out — while
 * leaving the splash alone.
 */
@Composable
@Preview
fun App(vm: RootViewModel = koinViewModel()) {
    val backStack = rememberNavBackStack(rootRouteSavedStateConfiguration, RootRoute.SplashKey)
    val userSession by vm.userSession.collectAsStateWithLifecycle(UserSession.Loading)

    // Only once there is somebody to notify: asked on the sign-in screen it would be a dialog about
    // an app the user has not seen yet, and a refusal there is one the system will not ask again.
    if (userSession is UserSession.Authenticated) RequestNotificationPermission()

    handleLogoutSignal(userSession, backStack)

    TurniaTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        val navigator = remember(backStack) { RootNavigator(backStack) }

        CompositionLocalProvider(
            LocalRootNavigator provides navigator,
            LocalNavigator provides navigator,
            LocalSnackbar provides snackbarHostState,
        ) {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { TurniaSnackbarHost(snackbarHostState) }
            ) {
                RootNavDisplay(
                    snackbarHostState = snackbarHostState,
                    backStack = backStack,
                )
            }
        }
    }
}

@Composable
private fun handleLogoutSignal(userSession: UserSession, backStack: NavBackStack<NavKey>) {
    LaunchedEffect(userSession) {
        if (backStack.lastOrNull() == RootRoute.SplashKey) return@LaunchedEffect
        when (userSession) {
            UserSession.Loading -> Unit
            // Main may be covered by a full-screen destination, so look for it in the whole stack.
            is UserSession.Authenticated -> if (RootRoute.MainKey !in backStack) {
                backStack.clear()
                backStack.add(RootRoute.MainKey)
            }

            UserSession.Unauthenticated -> if (backStack.lastOrNull() != RootRoute.SignInKey) {
                backStack.clear()
                backStack.add(RootRoute.SignInKey)
            }

        }
    }
}
