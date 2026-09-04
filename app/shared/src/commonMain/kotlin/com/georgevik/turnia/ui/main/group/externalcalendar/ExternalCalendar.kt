package com.georgevik.turnia.ui.main.group.externalcalendar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.georgevik.turnia.core.domain.model.GroupId
import com.georgevik.turnia.navigation.LocalNavigator
import com.georgevik.turnia.navigation.LocalRootNavigator
import com.georgevik.turnia.navigation.main.routes.ExternalCalendarData
import com.georgevik.turnia.navigation.root.routes.RootRoute
import com.georgevik.turnia.ui.components.calendar.CalendarThemes
import com.georgevik.turnia.ui.components.calendar.CalendarViewer
import com.georgevik.turnia.ui.components.daydetail.DayAddMode
import com.georgevik.turnia.ui.main.group.calendarlist.components.CalendarTitleBar

@Composable
fun ExternalCalendar(viewModel: ExternalCalendarViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    // Group detail covers the whole screen, so it goes on the root stack, not this tab's.
    val rootNavigator = LocalRootNavigator.current
    val data = viewModel.data
    val isGroup = data is ExternalCalendarData.Group
    val theme = if (isGroup) CalendarThemes.group() else CalendarThemes.colleague()
    val addMode = when (data) {
        is ExternalCalendarData.Group -> DayAddMode.GroupOnly(GroupId(data.id))
        is ExternalCalendarData.Personal -> DayAddMode.Disabled
    }

    CalendarViewer(
        theme = theme,
        addMode = addMode,
        titleBar = {
            CalendarTitleBar(
                title = data.name,
                icon = if (isGroup) Icons.Default.Groups else Icons.Default.Person,
                theme = theme,
                onBack = navigator::goBack,
                onTitleClick = if (data is ExternalCalendarData.Group) {
                    { rootNavigator.goTo(RootRoute.GroupDetailKey(data.id)) }
                } else {
                    null
                },
            )
        },
        onMonthChanged = viewModel::onMonthChanged,
        eventsByDate = uiState.events,
    )
}
