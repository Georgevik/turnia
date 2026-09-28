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
    fun moveType(id: EventTypeId) = "move_type_${id.value}"
    fun shiftSetupRow(index: Int) = "shift_setup_row_$index"
    fun shiftSetupExpand(index: Int) = "shift_setup_expand_$index"
    fun shiftSetupStart(index: Int) = "shift_setup_start_$index"
    fun shiftSetupEnd(index: Int) = "shift_setup_end_$index"

    const val SWAP_TOGGLE = "swap_toggle"
    const val MOVE_TO_GROUP = "move_to_group"
    const val MOVE_SHEET = "move_sheet"
    const val GROUPS_FAB = "groups_fab"
    const val JOIN_CODE_FIELD = "join_code_field"
    const val USER_SEARCH_FIELD = "user_search_field"
    const val DAY_SHEET = "day_sheet"
    const val ADD_PANE_SHIFTS = "add_pane_shifts"
    const val ADD_PANE_EMPTY_SHIFTS = "add_pane_empty_shifts"
    const val ADD_PANE_OTHER_EVENT = "add_pane_other_event"
    const val ONE_OFF_SAVE_AS_SHIFT = "one_off_save_as_shift"
    const val SHIFT_SETUP = "shift_setup"
    const val SHIFT_SETUP_CONFIRM = "shift_setup_confirm"
    const val SHIFT_SETUP_CUSTOM_NAME = "shift_setup_custom_name"
    const val SHIFT_SETUP_CUSTOM_ACRONYM = "shift_setup_custom_acronym"
    const val SHIFT_SETUP_CUSTOM_START = "shift_setup_custom_start"
    const val SHIFT_SETUP_CUSTOM_END = "shift_setup_custom_end"
}
