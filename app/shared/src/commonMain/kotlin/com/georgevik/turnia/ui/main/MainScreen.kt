package com.georgevik.turnia.ui.main

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
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
import com.georgevik.turnia.navigation.LocalNavigator
import com.georgevik.turnia.navigation.TurniaNavDisplay
import com.georgevik.turnia.navigation.main.MainNavigator
import com.georgevik.turnia.navigation.main.rememberMainNavigationState
import com.georgevik.turnia.navigation.main.routes.MainRoute
import com.georgevik.turnia.ui.main.changes.navigation.changesNavigation
import com.georgevik.turnia.ui.main.group.navigation.groupsNavigation
import com.georgevik.turnia.ui.main.mycalendar.navigation.calendarNavigation
import com.georgevik.turnia.ui.main.settings.navigation.settingsNavigation
import com.georgevik.turnia.ui.system.LocalSnackbar
import com.georgevik.turnia.ui.system.TurniaSnackbarHost
import org.koin.compose.viewmodel.koinViewModel

private data class MainTabBarItem(
    val route: MainRoute,
    val title: String,
    val icon: ImageVector,
    val requiresSwapFlag: Boolean = false,
)

private val MAIN_TABS = listOf(
    MainTabBarItem(MainRoute.CalendarTab, "Calendario", Icons.Default.CalendarMonth),
    MainTabBarItem(MainRoute.GroupsTab, "Grupos", Icons.Default.Groups),
    MainTabBarItem(MainRoute.ChangesTab, "Cambios", Icons.Default.SwapHoriz, requiresSwapFlag = true),
    MainTabBarItem(MainRoute.ProfileTab, "Perfil", Icons.Default.Person),
)

/**
 * Main: Calendar/Groups/Changes/Profile as independent back stacks (Changes is feature-flag
 * gated). See `MainNavigationState`/`MainNavigator` for how tab switching and back navigation
 * work — this screen just wires the bottom bar to them and hosts the single flattened
 * [TurniaNavDisplay].
 */
@Composable
fun MainScreen(viewModel: MainViewModel = koinViewModel()) {
    val featureFlags by viewModel.featureFlags.collectAsStateWithLifecycle()
    val visibleTabs = MAIN_TABS.filter { !it.requiresSwapFlag || featureFlags.showSwapTab }

    val state = rememberMainNavigationState(
        startRoute = MainRoute.CalendarTab,
        topLevelRoutes = remember { MAIN_TABS.map { it.route }.toSet() },
    )
    val navigator = remember(state) { MainNavigator(state) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.topLevelRoute, featureFlags.showSwapTab) {
        if (state.topLevelRoute == MainRoute.ChangesTab && !featureFlags.showSwapTab) {
            navigator.goTo(MainRoute.CalendarTab)
        }
    }

    CompositionLocalProvider(
        LocalNavigator provides navigator,
        LocalSnackbar provides snackbarHostState,
    ) {
        Scaffold(
            snackbarHost = { TurniaSnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                NavigationBar {
                    visibleTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = state.topLevelRoute == tab.route,
                            onClick = { navigator.goTo(tab.route) },
                            icon = {
                                Icon(imageVector = tab.icon, contentDescription = tab.title)
                            },
                            label = { Text(text = tab.title) },
                        )
                    }
                }
            }
        ) { innerPadding ->
            TurniaNavDisplay(
                entries = state.toDecoratedEntries(
                    entryProvider {
                        calendarNavigation()
                        groupsNavigation()
                        settingsNavigation()
                        changesNavigation()
                    }
                ),
                onBack = navigator::goBack,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
