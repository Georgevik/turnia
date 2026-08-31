package com.georgevik.turnia.ui.components.daydetail

/**
 * What a viewer may add from a day's detail sheet, which depends on whose
 * calendar is open:
 *  - [Full]: my own calendar — personal event types and every group I belong to.
 *  - [GroupOnly]: a group calendar — only that group's event types.
 *  - [Disabled]: another user's personal calendar — read-only, nothing can be added.
 */
sealed interface DayAddMode {
    data object Full : DayAddMode
    data class GroupOnly(val groupId: String) : DayAddMode
    data object Disabled : DayAddMode

    val canAdd: Boolean get() = this != Disabled
}
