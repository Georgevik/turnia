package com.georgevik.turnia.ui.root

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.rememberNavBackStack
import com.georgevik.turnia.core.domain.model.UserSession
import com.georgevik.turnia.navigation.root.RootNavDisplay
import com.georgevik.turnia.navigation.root.rootRouteSavedStateConfiguration
import com.georgevik.turnia.navigation.root.routes.RootRoute
import com.georgevik.turnia.ui.system.TurniaSnackbarVisual
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

    LaunchedEffect(userSession) {
        if (backStack.lastOrNull() == RootRoute.SplashKey) return@LaunchedEffect
        when (userSession) {
            UserSession.Loading -> Unit
            is UserSession.Authenticated -> if (backStack.lastOrNull() != RootRoute.MainKey) {
                backStack.clear()
                backStack.add(RootRoute.MainKey)
            }

            UserSession.Unauthenticated -> if (backStack.lastOrNull() != RootRoute.SignInKey) {
                backStack.clear()
                backStack.add(RootRoute.SignInKey)
            }
        }
    }

    TurniaTheme {
        val snackbarHostState = remember { SnackbarHostState() }

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    val isError = (data.visuals as? TurniaSnackbarVisual)?.isError ?: false

                    Snackbar(
                        snackbarData = data,
                        containerColor = if (isError) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            SnackbarDefaults.color
                        },
                        contentColor = if (isError) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            SnackbarDefaults.contentColor
                        }
                    )
                }
            }
        ) { innerPadding ->
            RootNavDisplay(
                snackbarHostState = snackbarHostState,
                backStack = backStack,
            )
        }
    }
}
