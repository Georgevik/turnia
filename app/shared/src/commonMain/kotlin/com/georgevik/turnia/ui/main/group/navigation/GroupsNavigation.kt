package com.georgevik.turnia.ui.main.group.navigation

import androidx.navigation3.runtime.EntryProviderScope
import com.georgevik.turnia.navigation.main.routes.MainRoute
import com.georgevik.turnia.ui.main.group.calendarlist.CalendarListScreen
import com.georgevik.turnia.ui.main.group.externalcalendar.ExternalCalendar
import com.georgevik.turnia.ui.main.group.externalcalendar.ExternalCalendarViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Groups tab: colleague/group list, and the calendar opened when one of them is tapped. */
fun EntryProviderScope<MainRoute>.groupsNavigation() {
    entry<MainRoute.GroupsTab> { CalendarListScreen() }

    entry<MainRoute.ExternalCalendar> { key ->
        ExternalCalendar(
            viewModel = koinViewModel<ExternalCalendarViewModel> { parametersOf(key.data) },
        )
    }
}
