package com.georgevik.turnia.ui.main.group.navigation

import androidx.navigation3.runtime.EntryProviderScope
import com.georgevik.turnia.navigation.main.routes.MainRoute
import com.georgevik.turnia.ui.main.group.externalcalendar.ExternalCalendar
import com.georgevik.turnia.ui.main.group.externalcalendar.ExternalCalendarViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** The calendar opened from either tab: a colleague's or a group's. */
fun EntryProviderScope<MainRoute>.externalCalendarNavigation() {
    entry<MainRoute.ExternalCalendar> { key ->
        ExternalCalendar(
            viewModel = koinViewModel<ExternalCalendarViewModel> { parametersOf(key.data) },
        )
    }
}
