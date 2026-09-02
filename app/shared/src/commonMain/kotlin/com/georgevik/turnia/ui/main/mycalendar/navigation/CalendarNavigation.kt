package com.georgevik.turnia.ui.main.mycalendar.navigation

import androidx.navigation3.runtime.EntryProviderScope
import com.georgevik.turnia.navigation.main.routes.MainRoute
import com.georgevik.turnia.ui.main.eventtypes.detail.EventTypeDetailScreen
import com.georgevik.turnia.ui.main.eventtypes.detail.EventTypeDetailViewModel
import com.georgevik.turnia.ui.main.mycalendar.MyCalendarScreen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

fun EntryProviderScope<MainRoute>.calendarNavigation() {
    entry<MainRoute.CalendarTab> { MyCalendarScreen() }

    entry<MainRoute.EventTypeDetailKey> { key ->
        val viewModel = koinViewModel<EventTypeDetailViewModel> {
            parametersOf(key.data)
        }
        EventTypeDetailScreen(viewModel = viewModel)
    }
}
