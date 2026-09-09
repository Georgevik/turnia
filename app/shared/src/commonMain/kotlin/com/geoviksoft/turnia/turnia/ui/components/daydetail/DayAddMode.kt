package com.geoviksoft.turnia.ui.components.daydetail

import com.geoviksoft.turnia.core.domain.model.GroupId

/**
 * What a viewer may add from a day's detail sheet, which depends on whose
 * calendar is open:
 *  - [Full]: my own calendar — personal event types and every group I belong to.
 *  - [GroupOnly]: a group calendar — only that group's event types.
 *  - [Disabled]: another user's personal calendar — read-only, nothing can be added.
 */
sealed interface DayAddMode {
    data object Full : DayAddMode
    data class GroupOnly(val groupId: GroupId) : DayAddMode
    data object Disabled : DayAddMode

    val canAdd: Boolean get() = this != Disabled
}