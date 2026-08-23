package com.georgevik.turnia.ui.main.group.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.georgevik.turnia.core.domain.model.CalendarKind
import com.georgevik.turnia.ui.components.calendar.CalendarThemes
import com.georgevik.turnia.ui.components.calendar.CalendarViewer
import com.georgevik.turnia.ui.main.group.model.GroupScreenUi.OpenCalendar

@Composable
fun OverlayCalendar(cal: OpenCalendar?, onBack: () -> Unit) {
    // Retain the last opened calendar so its content keeps rendering while the
    // exit animation plays. If we read `cal` directly, closing sets it to null on
    // the same frame the exit starts, so there's nothing left to slide out.
    var lastCal by remember { mutableStateOf(cal) }
    if (cal != null) lastCal = cal

    AnimatedVisibility(
        visible = cal != null,
        enter = slideInHorizontally { it },
        exit = slideOutHorizontally { it }
    ) {
        lastCal?.let { calendar ->
            val isGroup = calendar.kind == CalendarKind.GROUP
            val theme = if (isGroup) CalendarThemes.group() else CalendarThemes.colleague()
            CalendarViewer(
                theme = theme,
                titleBar = {
                    CalendarTitleBar(
                        title = calendar.name,
                        icon = if (isGroup) Icons.Default.Groups else Icons.Default.Person,
                        theme = theme,
                        onBack = { onBack() },
                    )

                },
                eventsByDate = calendar.events,
            )
        }
    }
}
