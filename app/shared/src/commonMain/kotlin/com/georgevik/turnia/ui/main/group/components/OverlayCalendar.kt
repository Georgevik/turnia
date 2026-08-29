package com.georgevik.turnia.ui.main.group.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import com.georgevik.turnia.core.domain.model.CalendarKind
import com.georgevik.turnia.ui.components.calendar.CalendarThemes
import com.georgevik.turnia.ui.components.calendar.CalendarViewer
import com.georgevik.turnia.ui.components.calendar.model.CalendarEventUi
import kotlinx.datetime.LocalDate

@Composable
fun OverlayCalendar(
    name: String,
    kind: CalendarKind,
    events: Map<LocalDate, List<CalendarEventUi>>,
    onBack: () -> Unit,
) {
    val isGroup = kind == CalendarKind.GROUP
    val theme = if (isGroup) CalendarThemes.group() else CalendarThemes.colleague()
    CalendarViewer(
        theme = theme,
        titleBar = {
            CalendarTitleBar(
                title = name,
                icon = if (isGroup) Icons.Default.Groups else Icons.Default.Person,
                theme = theme,
                onBack = onBack,
            )
        },
        eventsByDate = events,
    )
}
