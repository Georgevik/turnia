package com.georgevik.turnia.ui.main.group.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.georgevik.turnia.navigation.main.routes.MainRoute
import com.georgevik.turnia.ui.main.group.calendarlist.CalendarListScreen
import com.georgevik.turnia.ui.main.group.externalcalendar.ExternalCalendar
import com.georgevik.turnia.ui.main.group.externalcalendar.ExternalCalendarViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Groups tab: colleague/group list, and the calendar opened when one of them is tapped. */
fun EntryProviderScope<NavKey>.groupsNavigation() {
    entry<MainRoute.GroupsTab> { CalendarListScreen() }

    entry<MainRoute.GroupCalendar> { key ->
        ExternalCalendar(
            viewModel = koinViewModel<ExternalCalendarViewModel> {
                parametersOf(key.id, key.name, key.kind)
            })
    }
}
