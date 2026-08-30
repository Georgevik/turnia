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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation3.runtime.rememberNavBackStack
import com.georgevik.turnia.core.domain.model.UserSession
import com.georgevik.turnia.navigation.root.RootNavDisplay
import com.georgevik.turnia.navigation.root.routes.RootRoute
import com.georgevik.turnia.navigation.root.rootRouteSavedStateConfiguration
import com.georgevik.turnia.ui.system.TurniaSnackbarVisual
import com.georgevik.turnia.ui.system.TurniaTheme
import org.koin.compose.viewmodel.koinViewModel

/**
 * The single place that decides Splash -> Main vs. Splash -> SignIn, and also resets back to
 * SignIn if the user gets signed out while deep in the app. Both are just branches of the same
 * [UserSession]-driven effect, so there's one decision point instead of one split across this
 * screen and the splash flow.
 */
@Composable
@Preview
fun App(vm: RootViewModel = koinViewModel()) {
    val backStack = rememberNavBackStack(rootRouteSavedStateConfiguration, RootRoute.SplashKey)
    val lifecycleOwner = rememberLifecycleOwner()
    val userSession by vm.userSession.flowWithLifecycle(lifecycleOwner.lifecycle)
        .collectAsStateWithLifecycle(UserSession.Loading)
    var splashMinDurationElapsed by remember { mutableStateOf(false) }

    LaunchedEffect(userSession, splashMinDurationElapsed) {
        val onSplash = backStack.lastOrNull() == RootRoute.SplashKey
        when (userSession) {
            UserSession.Loading -> Unit
            is UserSession.Authenticated -> if (!onSplash || splashMinDurationElapsed) {
                backStack.clear()
                backStack.add(RootRoute.MainKey)
            }
            UserSession.Unauthenticated -> if (!onSplash || splashMinDurationElapsed) {
                backStack.clear()
                backStack.add(RootRoute.SignInKey)
            }
        }
    }

    TurniaTheme {
        val snackbarHostState = remember { SnackbarHostState() }

        Scaffold(
            contentWindowInsets = WindowInsets(0,0,0,0),
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
                onSplashMinimumDurationElapsed = { splashMinDurationElapsed = true },
            )
        }
    }
}
