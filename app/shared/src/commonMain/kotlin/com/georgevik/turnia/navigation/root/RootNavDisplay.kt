package com.georgevik.turnia.navigation.root

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.georgevik.turnia.navigation.TrackScreen
import com.georgevik.turnia.navigation.TurniaNavDisplay
import com.georgevik.turnia.navigation.root.routes.RootRoute
import com.georgevik.turnia.ui.main.MainScreen
import com.georgevik.turnia.ui.main.eventtypes.detail.EventTypeDetailScreen
import com.georgevik.turnia.ui.main.eventtypes.detail.EventTypeDetailViewModel
import com.georgevik.turnia.ui.main.eventtypes.personal.PersonalEventTypesScreen
import com.georgevik.turnia.ui.main.notifications.NotificationsScreen
import com.georgevik.turnia.ui.main.profile.MyProfileScreen
import com.georgevik.turnia.ui.main.sharecalendar.ShareCalendarScreen
import com.georgevik.turnia.ui.signin.SignInScreen
import com.georgevik.turnia.ui.splash.SplashScreen
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
            entry<RootRoute.SignInKey> { SignInScreen() }
            entry<RootRoute.MainKey> { MainScreen() }

            entry<RootRoute.EventTypeDetailKey> { key ->
                EventTypeDetailScreen(
                    viewModel = koinViewModel<EventTypeDetailViewModel> { parametersOf(key.data) },
                )
            }

            entry<RootRoute.PersonalEventTypesKey> { PersonalEventTypesScreen(viewModel = koinViewModel()) }

            entry<RootRoute.MyProfileKey> { MyProfileScreen() }

            entry<RootRoute.ShareCalendarKey> { ShareCalendarScreen() }

            entry<RootRoute.NotificationsKey> { NotificationsScreen() }
        },
    )
}
