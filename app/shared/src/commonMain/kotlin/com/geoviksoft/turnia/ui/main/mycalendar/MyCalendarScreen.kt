package com.geoviksoft.turnia.ui.main.mycalendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geoviksoft.turnia.navigation.LocalNavigator
import com.geoviksoft.turnia.navigation.LocalRootNavigator
import com.geoviksoft.turnia.navigation.main.routes.MainRoute
import com.geoviksoft.turnia.navigation.root.routes.RootRoute
import com.geoviksoft.turnia.navigation.routes.EventTypeDetailData
import com.geoviksoft.turnia.ui.components.calendar.CalendarViewer
import com.geoviksoft.turnia.ui.components.daydetail.DayAddMode
import com.geoviksoft.turnia.ui.system.RequestNotificationPermission
import com.geoviksoft.turnia.ui.system.ads.AdBanner
import org.koin.compose.viewmodel.koinViewModel

/**
 * "Calendario" tab. Renders a single month grid. Event data is date-based
 * (no time, no time zones); the only clock read is resolving "today".
 */
@Composable
fun MyCalendarScreen(viewModel: MyCalendarViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    val rootNavigator = LocalRootNavigator.current

    // Here rather than at the root: on first launch the splash is still asking for ads consent, and
    // two system dialogs on top of each other is a bad first impression.
    RequestNotificationPermission()

    Column(modifier = Modifier.fillMaxSize()) {
        CalendarViewer(
            modifier = Modifier.weight(1f),
            eventsByDate = (uiState as? MyCalendarUiState.Success)?.eventsByDate.orEmpty(),
            oneOffsByDate = (uiState as? MyCalendarUiState.Success)?.oneOffsByDate.orEmpty(),
            isLoading = uiState is MyCalendarUiState.Loading,
            onMonthChanged = viewModel::onMonthChanged,
            addMode = DayAddMode.Full,
            onEditGroup = { groupId, _ ->
                navigator.goTo(MainRoute.GroupDetail(groupId))
            },
            onAddPersonalType = {
                rootNavigator.goTo(RootRoute.EventTypeDetailKey(EventTypeDetailData.NewPersonal))
            },
            onAddGroupType = { groupId ->
                rootNavigator.goTo(RootRoute.EventTypeDetailKey(EventTypeDetailData.NewGroup(groupId)))
            },
        )
        AdBanner()
    }
}
