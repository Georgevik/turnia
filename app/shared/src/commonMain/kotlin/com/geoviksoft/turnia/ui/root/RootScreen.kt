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
import com.geoviksoft.turnia.core.system.BuildInfo
import com.geoviksoft.turnia.navigation.LocalNavigator
import com.geoviksoft.turnia.navigation.LocalRootNavigator
import com.geoviksoft.turnia.navigation.root.RootNavDisplay
import com.geoviksoft.turnia.navigation.root.RootNavigator
import com.geoviksoft.turnia.navigation.root.rootRouteSavedStateConfiguration
import com.geoviksoft.turnia.navigation.root.routes.RootRoute
import com.geoviksoft.turnia.ui.root.name.CompleteNameDialog
import com.geoviksoft.turnia.ui.system.AppLanguageHost
import com.geoviksoft.turnia.ui.system.LocalBuildInfo
import com.geoviksoft.turnia.ui.system.LocalSnackbar
import com.geoviksoft.turnia.ui.system.LocalTextSharer
import com.geoviksoft.turnia.ui.system.TextSharer
import com.geoviksoft.turnia.ui.system.TurniaSnackbarHost
import com.geoviksoft.turnia.ui.system.TurniaTheme
import com.geoviksoft.turnia.ui.system.rememberTextSharer
import org.koin.compose.getKoin
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Hosts the root back stack. The splash owns the initial hand-off (Main vs. SignIn) once its
 * minimum duration elapses and the session resolves; this effect then keeps the app in sync with
 * later [UserSession] changes — routing to Main on sign-in and back to SignIn on sign-out — while
 * leaving the splash alone.
 */
@Composable
fun App(vm: RootViewModel = koinViewModel()) {
    val userSession by vm.userSession.collectAsStateWithLifecycle(UserSession.Loading)
    val pendingJoinCode by vm.pendingJoinCode.collectAsStateWithLifecycle()
    val buildInfo: BuildInfo = koinInject()
    // Bound only by the E2E suite, to see what would have been shared.
    val textSharer: TextSharer? = getKoin().getOrNull()

    AppContent(userSession, pendingJoinCode, buildInfo, textSharer)
}

@Composable
fun AppContent(
    userSession: UserSession,
    pendingJoinCode: String?,
    buildInfo: BuildInfo,
    textSharer: TextSharer? = null,
) {
    val backStack = rememberNavBackStack(rootRouteSavedStateConfiguration, RootRoute.SplashKey)

    HandleLogoutSignal(userSession, backStack)
    HandleJoinGroupDeeplink(pendingJoinCode, backStack)

    TurniaTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        val navigator = remember(backStack) { RootNavigator(backStack) }

        CompositionLocalProvider(
            LocalRootNavigator provides navigator,
            LocalNavigator provides navigator,
            LocalSnackbar provides snackbarHostState,
            LocalBuildInfo provides buildInfo,
            LocalTextSharer provides (textSharer ?: rememberTextSharer()),
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

private val SignedOutRoutes: Set<NavKey> = setOf(RootRoute.OnboardingKey, RootRoute.SignInKey)

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

            UserSession.Unauthenticated -> if (backStack.lastOrNull() !in SignedOutRoutes) {
                backStack.clear()
                backStack.add(RootRoute.SignInKey)
            }

        }
    }
}

@Preview
@Composable
fun AppContentPreview() {
    AppContent(UserSession.Loading, null, BuildInfo(isDebug = false))
}
