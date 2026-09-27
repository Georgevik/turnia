package com.geoviksoft.turnia.ui.system

import com.geoviksoft.turnia.core.domain.model.EventId
import com.geoviksoft.turnia.core.domain.model.EventTypeId
import com.geoviksoft.turnia.core.domain.model.UserId
import kotlinx.datetime.LocalDate

/**
 * Handles the Android E2E tests find nodes by, where the text on screen cannot tell them apart: a
 * day number repeats across the pager's months, and icon-only controls have no label. Renaming one
 * breaks `app/androidApp/src/androidTest`.
 */
object TestTags {
    fun day(date: LocalDate) = "day_$date"
    fun dayEvent(id: EventId) = "event_${id.value}"
    fun swapEvent(id: EventId) = "swap_event_${id.value}"
    fun eventTypeChip(id: EventTypeId) = "event_type_${id.value}"
    fun personRow(id: UserId) = "person_row_${id.value}"

    const val SWAP_TOGGLE = "swap_toggle"
    const val GROUPS_FAB = "groups_fab"
    const val JOIN_CODE_FIELD = "join_code_field"
    const val USER_SEARCH_FIELD = "user_search_field"
    const val DAY_SHEET = "day_sheet"
}
