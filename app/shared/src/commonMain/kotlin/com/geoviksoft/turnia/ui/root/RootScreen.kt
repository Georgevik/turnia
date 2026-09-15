package com.geoviksoft.turnia.ui.root

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
import com.geoviksoft.turnia.core.domain.model.UserSession
import com.geoviksoft.turnia.navigation.LocalNavigator
import com.geoviksoft.turnia.navigation.LocalRootNavigator
import com.geoviksoft.turnia.navigation.root.RootNavDisplay
import com.geoviksoft.turnia.navigation.root.RootNavigator
import com.geoviksoft.turnia.navigation.root.rootRouteSavedStateConfiguration
import com.geoviksoft.turnia.navigation.root.routes.RootRoute
import com.geoviksoft.turnia.ui.root.name.CompleteNameDialog
import com.geoviksoft.turnia.ui.system.AppLanguageHost
import com.geoviksoft.turnia.ui.system.LocalSnackbar
import com.geoviksoft.turnia.ui.system.TurniaSnackbarHost
import com.geoviksoft.turnia.ui.system.TurniaTheme
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
    val pendingJoinCode by vm.pendingJoinCode.collectAsStateWithLifecycle()

    HandleLogoutSignal(userSession, backStack)
    HandleJoinGroupDeeplink(pendingJoinCode, backStack)

    TurniaTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        val navigator = remember(backStack) { RootNavigator(backStack) }

        CompositionLocalProvider(
            LocalRootNavigator provides navigator,
            LocalNavigator provides navigator,
            LocalSnackbar provides snackbarHostState,
        ) {
            AppLanguageHost {
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = { TurniaSnackbarHost(snackbarHostState) }
                ) {
                    RootNavDisplay(
                        snackbarHostState = snackbarHostState,
                        backStack = backStack,
                    )

                    // Over whatever the session routed to: nobody gets past it without a name.
                    if ((userSession as? UserSession.Authenticated)?.user?.needsName == true) {
                        CompleteNameDialog()
                    }
                }
            }
        }
    }
}

/**
 * An invitation link lands on Main's Groups tab, which a full-screen page opened from Main
 * (an event type, a settings page) would otherwise keep covered. Before sign-in there is no Main
 * yet, and the code simply waits for it.
 */
@Composable
private fun HandleJoinGroupDeeplink(code: String?, backStack: NavBackStack<NavKey>) {
    LaunchedEffect(code, backStack.size) {
        if (code == null || RootRoute.MainKey !in backStack) return@LaunchedEffect
        while (backStack.last() != RootRoute.MainKey) backStack.removeLastOrNull()
    }
}

@Composable
private fun HandleLogoutSignal(userSession: UserSession, backStack: NavBackStack<NavKey>) {
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
