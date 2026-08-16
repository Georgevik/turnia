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
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation3.runtime.rememberNavBackStack
import com.georgevik.turnia.core.domain.model.UserSession
import com.georgevik.turnia.navigation.Route
import com.georgevik.turnia.navigation.TurniaNavDisplay
import com.georgevik.turnia.navigation.navKeySavedStateConfiguration
import com.georgevik.turnia.ui.system.TurniaSnackbarVisual
import com.georgevik.turnia.ui.system.TurniaTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
@Preview
fun App(vm: RootViewModel = koinViewModel()) {
    val backStack = rememberNavBackStack(navKeySavedStateConfiguration, Route.SpashKey)
    val lifecycleOwner = rememberLifecycleOwner()

    LaunchedEffect(Unit) {
        vm.userSession.flowWithLifecycle(lifecycleOwner.lifecycle).collect { userSession ->
            when (userSession) {
                UserSession.Unauthenticated -> {
                    backStack.clear()
                    backStack.add(Route.SpashKey)
                }
                is UserSession.Authenticated,
                UserSession.Loading -> Unit
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
            TurniaNavDisplay(snackbarHostState, backStack)
        }
    }
}
