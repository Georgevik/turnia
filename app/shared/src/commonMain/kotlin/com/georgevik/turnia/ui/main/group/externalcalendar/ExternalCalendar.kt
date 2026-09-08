package com.georgevik.turnia.ui.main.group.externalcalendar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.georgevik.turnia.core.domain.model.GroupId
import com.georgevik.turnia.navigation.LocalNavigator
import com.georgevik.turnia.navigation.main.routes.ExternalCalendarData
import com.georgevik.turnia.navigation.main.routes.MainRoute
import com.georgevik.turnia.ui.components.calendar.CalendarThemes
import com.georgevik.turnia.ui.components.calendar.CalendarViewer
import com.georgevik.turnia.ui.components.calendar.components.CalendarTitleBar
import com.georgevik.turnia.ui.components.daydetail.DayAddMode

/**
 * A calendar that is not the user's own: a group's, or a colleague's. Read and add only — leaving
 * and deleting the group belong to its detail, behind the info button.
 */
@Composable
fun ExternalCalendar(viewModel: ExternalCalendarViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    val data = viewModel.data
    val isGroup = data is ExternalCalendarData.Group
    val theme = if (isGroup) CalendarThemes.group() else CalendarThemes.colleague()

    // Someone removed from the group keeps their leftover shifts on screen but adds nothing more,
    // and the group's detail is closed to them along with the group document it reads.
    val addMode = when {
        data !is ExternalCalendarData.Group -> DayAddMode.Disabled
        uiState.isRevoked -> DayAddMode.Disabled
        else -> DayAddMode.GroupOnly(GroupId(data.id))
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
                onInfo = if (data is ExternalCalendarData.Group && !uiState.isRevoked) {
                    { navigator.goTo(MainRoute.GroupDetail(data.id)) }
                } else {
                    null
                },
            )
        },
        onMonthChanged = viewModel::onMonthChanged,
        eventsByDate = uiState.events,
    )
}
