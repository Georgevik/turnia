package com.georgevik.turnia.ui.main.group.model

import com.georgevik.turnia.core.domain.model.CalendarKind
import com.georgevik.turnia.ui.components.calendar.model.CalendarEventUi
import kotlinx.datetime.LocalDate


data class GroupScreenUi(
    val groups: List<GroupRowUi>,
    val colleagues: List<ColleageRowUi>,
    val showLoading: Boolean,
    val openCalendar: OpenCalendar?
) {
    data class OpenCalendar(
        val id: String,
        val name: String,
        val kind: CalendarKind,
        val events: Map<LocalDate, List<CalendarEventUi>>
    )
}
