package com.geoviksoft.turnia.navigation.root

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.geoviksoft.turnia.navigation.TrackScreen
import com.geoviksoft.turnia.navigation.TurniaNavDisplay
import com.geoviksoft.turnia.navigation.root.routes.RootRoute
import com.geoviksoft.turnia.ui.main.MainScreen
import com.geoviksoft.turnia.ui.main.about.AboutScreen
import com.geoviksoft.turnia.ui.main.eventtypes.detail.EventTypeDetailScreen
import com.geoviksoft.turnia.ui.main.eventtypes.detail.EventTypeDetailViewModel
import com.geoviksoft.turnia.ui.main.eventtypes.personal.PersonalEventTypesScreen
import com.geoviksoft.turnia.ui.main.preferences.PreferencesScreen
import com.geoviksoft.turnia.ui.main.profile.MyProfileScreen
import com.geoviksoft.turnia.ui.onboarding.OnboardingScreen
import com.geoviksoft.turnia.ui.signin.SignInScreen
import com.geoviksoft.turnia.ui.signin.createaccount.CreateAccountScreen
import com.geoviksoft.turnia.ui.splash.SplashScreen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun RootNavDisplay(
    snackbarHostState: SnackbarHostState,
    backStack: NavBackStack<NavKey>,
) {
    TrackScreen(backStack.lastOrNull())

    TurniaNavDisplay(
        backStack = backStack,
        entryProvider = entryProvider {
            entry<RootRoute.SplashKey> {
                SplashScreen(backStack = backStack, snackbar = snackbarHostState)
            }
            entry<RootRoute.OnboardingKey> {
                OnboardingScreen(
                    onFinished = {
                        backStack.clear()
                        backStack.add(RootRoute.SignInKey)
                    },
                )
            }
            entry<RootRoute.SignInKey> { SignInScreen() }
            entry<RootRoute.CreateAccountKey> { CreateAccountScreen() }
            entry<RootRoute.MainKey> { MainScreen() }

            entry<RootRoute.EventTypeDetailKey> { key ->
                EventTypeDetailScreen(
                    viewModel = koinViewModel<EventTypeDetailViewModel> { parametersOf(key.data) },
                )
            }

            entry<RootRoute.PersonalEventTypesKey> { PersonalEventTypesScreen(viewModel = koinViewModel()) }

            entry<RootRoute.MyProfileKey> { MyProfileScreen() }

            entry<RootRoute.PreferencesKey> { PreferencesScreen() }

            entry<RootRoute.AboutKey> { AboutScreen() }
        },
    )
}
