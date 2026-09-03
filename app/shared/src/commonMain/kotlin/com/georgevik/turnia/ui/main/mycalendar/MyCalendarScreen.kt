package com.georgevik.turnia.ui.main.mycalendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.georgevik.turnia.navigation.LocalRootNavigator
import com.georgevik.turnia.navigation.root.routes.RootRoute
import com.georgevik.turnia.navigation.routes.EventTypeDetailData
import com.georgevik.turnia.ui.components.calendar.CalendarViewer
import com.georgevik.turnia.ui.components.daydetail.DayAddMode
import org.koin.compose.viewmodel.koinViewModel

/**
 * "Calendario" tab. Renders a single month grid. Event data is date-based
 * (no time, no time zones); the only clock read is resolving "today".
 */
@Composable
fun MyCalendarScreen(viewModel: MyCalendarViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Group detail covers the whole screen, so it goes on the root stack, not this tab's.
    val rootNavigator = LocalRootNavigator.current

    CalendarViewer(
        eventsByDate = uiState.eventsByDate,
        onMonthChanged = viewModel::onMonthChanged,
        addMode = DayAddMode.Full,
        onEditGroup = { groupId, _ ->
            rootNavigator.goTo(RootRoute.GroupDetailKey(groupId))
        },
        onAddPersonalType = {
            rootNavigator.goTo(RootRoute.EventTypeDetailKey(EventTypeDetailData.NewPersonal))
        },
    )
}
