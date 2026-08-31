package com.georgevik.turnia.ui.main.mycalendar.navigation

import androidx.navigation3.runtime.EntryProviderScope
import com.georgevik.turnia.navigation.main.routes.MainRoute
import com.georgevik.turnia.ui.main.eventtypes.EventMasterScreen
import com.georgevik.turnia.ui.main.eventtypes.EventMasterViewModel
import com.georgevik.turnia.ui.main.eventtypes.detail.EventTypeDetailScreen
import com.georgevik.turnia.ui.main.eventtypes.detail.EventTypeDetailViewModel
import com.georgevik.turnia.ui.main.mycalendar.MyCalendarScreen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

fun EntryProviderScope<MainRoute>.calendarNavigation() {
    entry<MainRoute.CalendarTab> { MyCalendarScreen() }

    entry<MainRoute.EventMasterKey> { key ->
        val viewModel = koinViewModel<EventMasterViewModel> {
            parametersOf(key.groupId, key.groupName)
        }
        EventMasterScreen(viewModel = viewModel)
    }

    entry<MainRoute.EventTypeDetailKey> { key ->
        val viewModel = koinViewModel<EventTypeDetailViewModel> {
            parametersOf(key.kind, key.groupId, key.typeId)
        }
        EventTypeDetailScreen(viewModel = viewModel)
    }
}
