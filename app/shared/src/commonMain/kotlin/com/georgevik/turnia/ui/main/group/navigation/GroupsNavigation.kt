package com.georgevik.turnia.ui.main.group.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.georgevik.turnia.navigation.LocalNavigator
import com.georgevik.turnia.navigation.MainRoute
import com.georgevik.turnia.ui.main.group.GroupCalendarViewModel
import com.georgevik.turnia.ui.main.group.GroupScreen
import com.georgevik.turnia.ui.main.group.components.OverlayCalendar
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Groups tab: colleague/group list, and the calendar opened when one of them is tapped. */
fun EntryProviderScope<NavKey>.groupsNavigation() {
    entry<MainRoute.GroupsTab> { GroupScreen() }

    entry<MainRoute.GroupCalendarKey> { key ->
        val viewModel = koinViewModel<GroupCalendarViewModel> {
            parametersOf(key.id, key.name, key.kind)
        }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val navigator = LocalNavigator.current
        OverlayCalendar(
            name = viewModel.name,
            kind = viewModel.kind,
            events = uiState.events,
            onBack = navigator::goBack,
        )
    }
}
