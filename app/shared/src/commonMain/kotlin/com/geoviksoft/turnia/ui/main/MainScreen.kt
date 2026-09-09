package com.geoviksoft.turnia.ui.main

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import com.geoviksoft.turnia.core.domain.model.PushDestination
import com.geoviksoft.turnia.navigation.LocalNavigator
import com.geoviksoft.turnia.navigation.TrackScreen
import com.geoviksoft.turnia.navigation.TurniaNavDisplay
import com.geoviksoft.turnia.navigation.main.MainNavigator
import com.geoviksoft.turnia.navigation.main.rememberMainNavigationState
import com.geoviksoft.turnia.navigation.main.routes.MainRoute
import com.geoviksoft.turnia.ui.group.detail.navigation.groupDetailNavigation
import com.geoviksoft.turnia.ui.main.swap.navigation.swapNavigation
import com.geoviksoft.turnia.ui.main.group.navigation.externalCalendarNavigation
import com.geoviksoft.turnia.ui.main.groups.navigation.groupsNavigation
import com.geoviksoft.turnia.ui.main.mycalendar.navigation.calendarNavigation
import com.geoviksoft.turnia.ui.main.people.navigation.peopleNavigation
import com.geoviksoft.turnia.ui.main.settings.navigation.settingsNavigation
import com.geoviksoft.turnia.ui.system.LocalSnackbar
import com.geoviksoft.turnia.ui.system.TurniaSnackbarHost
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.tab_calendar
import turnia.app.shared.generated.resources.tab_swap
import turnia.app.shared.generated.resources.tab_groups
import turnia.app.shared.generated.resources.tab_people
import turnia.app.shared.generated.resources.tab_settings

private data class MainTabBarItem(
    val route: MainRoute,
    /** A resource and not a resolved string: this list outlives a language change. */
    val title: StringResource,
    val icon: ImageVector,
)

private val MAIN_TABS = listOf(
    MainTabBarItem(MainRoute.CalendarTab, Res.string.tab_calendar, Icons.Default.CalendarMonth),
    MainTabBarItem(MainRoute.PeopleTab, Res.string.tab_people, Icons.Default.People),
    MainTabBarItem(MainRoute.GroupsTab, Res.string.tab_groups, Icons.Default.Groups),
    MainTabBarItem(MainRoute.SwapTab, Res.string.tab_swap, Icons.Default.SwapHoriz),
    MainTabBarItem(MainRoute.SettingsMenuTab, Res.string.tab_settings, Icons.Default.Settings),
)

/**
 * Main: Calendar/People/Groups/Swap/Settings as independent back stacks. See
 * `MainNavigationState`/`MainNavigator` for how tab switching and back navigation work — this
 * screen just wires the bottom bar to them and hosts the single flattened [TurniaNavDisplay].
 */
@Composable
fun MainScreen(viewModel: MainViewModel = koinViewModel()) {
    val state = rememberMainNavigationState(
        startRoute = MainRoute.CalendarTab,
        topLevelRoutes = remember { MAIN_TABS.map { it.route }.toSet() },
    )
    val navigator = remember(state) { MainNavigator(state) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Where a tapped notification asks to go. Every destination is one of Main's, so a cold start
    // takes care of itself: this screen only exists once the splash has handed over to Main.
    val pendingDestination by viewModel.pendingDestination.collectAsStateWithLifecycle()
    HandleNotificationTapped(pendingDestination, viewModel::destinationHandled, navigator)

    TrackScreen(state.backStacks[state.topLevelRoute]?.lastOrNull())

    CompositionLocalProvider(
        LocalNavigator provides navigator,
        LocalSnackbar provides snackbarHostState,
    ) {
        Scaffold(
            snackbarHost = { TurniaSnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                NavigationBar {
                    MAIN_TABS.forEach { tab ->
                        val title = stringResource(tab.title)
                        NavigationBarItem(
                            selected = state.topLevelRoute == tab.route,
                            onClick = { navigator.goTo(tab.route) },
                            icon = { Icon(imageVector = tab.icon, contentDescription = title) },
                            label = { Text(text = title) },
                        )
                    }
                }
            }
        ) { innerPadding ->
            TurniaNavDisplay(
                entries = state.toDecoratedEntries(
                    entryProvider {
                        calendarNavigation()
                        peopleNavigation()
                        groupsNavigation()
                        externalCalendarNavigation()
                        settingsNavigation()
                        swapNavigation()
                        groupDetailNavigation()
                    }
                ),
                onBack = navigator::goBack,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun HandleNotificationTapped(
    pendingDestination: PushDestination?,
    notifHandled : () -> Unit,
    navigator: MainNavigator
) {
    LaunchedEffect(pendingDestination) {
        val route = when (pendingDestination) {
            PushDestination.Groups -> MainRoute.GroupsTab
            PushDestination.People -> MainRoute.PeopleTab
            is PushDestination.GroupDetail ->
                MainRoute.GroupDetail(pendingDestination.groupId.value)

            null -> return@LaunchedEffect
        }

        notifHandled()
        navigator.goTo(route)
    }
}
