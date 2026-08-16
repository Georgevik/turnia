package com.georgevik.turnia.ui.main.mycalendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.georgevik.turnia.ui.components.calendar.CalendarViewer
import org.koin.compose.viewmodel.koinViewModel

/**
 * "Calendario" tab. Renders a single month grid. Event data is date-based
 * (no time, no time zones); the only clock read is resolving "today".
 */
@Composable
fun MyCalendarScreen(viewModel: MyCalendarViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CalendarViewer(eventsByDate = uiState.eventsByDate)
}
