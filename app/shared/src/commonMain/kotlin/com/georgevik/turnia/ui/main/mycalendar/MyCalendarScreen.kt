package com.georgevik.turnia.ui.main.mycalendar

import androidx.compose.runtime.Composable
import com.georgevik.turnia.ui.components.calendar.CalendarViewer

/**
 * "Calendario" tab. Renders a single month grid. Event data is date-based
 * (no time, no time zones); the only clock read is resolving "today".
 */
@Composable
fun MyCalendarScreen() {
    CalendarViewer()
}
