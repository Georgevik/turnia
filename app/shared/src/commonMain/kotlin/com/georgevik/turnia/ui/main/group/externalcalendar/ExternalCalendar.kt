package com.georgevik.turnia.ui.main.group.externalcalendar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.georgevik.turnia.navigation.LocalNavigator
import com.georgevik.turnia.navigation.main.routes.ExternalCalendarData
import com.georgevik.turnia.ui.components.calendar.CalendarThemes
import com.georgevik.turnia.ui.components.calendar.CalendarViewer
import com.georgevik.turnia.ui.main.group.calendarlist.components.CalendarTitleBar

@Composable
fun ExternalCalendar(viewModel: ExternalCalendarViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    val isGroup = viewModel.data is ExternalCalendarData.Group
    val theme = if (isGroup) CalendarThemes.group() else CalendarThemes.colleague()

    CalendarViewer(
        theme = theme,
        titleBar = {
            CalendarTitleBar(
                title = viewModel.data.name,
                icon = if (isGroup) Icons.Default.Groups else Icons.Default.Person,
                theme = theme,
                onBack = navigator::goBack,
            )
        },
        onMonthChanged = viewModel::onMonthChanged,
        invalidateEvents = viewModel::invalidateData,
        eventsByDate = uiState.events,
    )
}
