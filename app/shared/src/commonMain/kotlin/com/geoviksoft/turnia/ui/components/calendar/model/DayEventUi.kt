package com.geoviksoft.turnia.ui.components.calendar.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.geoviksoft.turnia.core.domain.model.EventId
import com.geoviksoft.turnia.core.domain.model.EventType
import com.geoviksoft.turnia.core.domain.model.GroupEvent
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.PersonalEvent
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.system.toLocalDate
import com.geoviksoft.turnia.ui.system.color.entityColor
import com.geoviksoft.turnia.ui.system.color.readableTextColor
import com.geoviksoft.turnia.ui.system.color.toComposeColorOrNull
import kotlinx.datetime.LocalDate

enum class EventSource { GROUP, PERSONAL }

data class TransferHolderUi(
    val name: String,
    val isMe: Boolean,
)

/**
 * An event as a row in a day's sheet — the whole of it, since that is the one place with room to
 * show a shift in full and offer what can be done about it.
 *
 * The month grid gets [cell] instead. Splitting them is not tidiness: a tile can show five things,
 * and a model that carries twenty-two makes it impossible to tell, from the cell's code, which of
 * them matter to it.
 */
@Immutable
data class DayEventUi(
    val id: EventId,
    /** Only a group event has these, and deleting one needs them. */
    val groupId: GroupId?,
    val ownerId: UserId?,
    val assigneeId: UserId?,
    val source: EventSource,
    val name: String,
    val acronym: String?,
    val background: Color,
    val date: LocalDate,
    val timeRange: String? = null,
    val onSwap: Boolean = false,
    /** Whether the event's type allows swapping at all. Always false for a personal event. */
    val swappable: Boolean = false,
    /**
     * Whether the viewer is an active member of the group this shift belongs to.
     *
     * Not derivable from the fields around it, and the gate on every swap action. The same event
     * renders on a colleague's shared calendar, where the viewer may be outside the group entirely,
     * and on the calendar of somebody removed from it who still holds shifts there — both are
     * refused by the security rules, so neither should be offered the control. It defaults to false
     * so a new caller has to opt in rather than accidentally show a button that fails.
     */
    val activeMember: Boolean = false,
    val isOwner: Boolean = false,
    val assigneeName: String = "",
    val assigneeIsMe: Boolean = false,
    val groupName: String? = null,
    val transferChain: List<TransferHolderUi> = emptyList(),
    val removable: Boolean = false,
    val notes: String? = null,
    val notesEditable: Boolean = false,
) {
    val textColor: Color = background.readableTextColor()

    /** This user created the shift and somebody else covers it now. */
    val assignedToOther: Boolean get() = !assigneeIsMe && isOwner

    /**
     * Whether this user may offer the shift, or withdraw the offer.
     *
     * Covering it is the whole condition: someone who took it from another member may pass it on,
     * and once it has been handed away [assigneeIsMe] is false, so the control disappears on its own
     * while the shift stays on the calendar of whoever created it.
     */
    val canOfferSwap: Boolean
        get() = source == EventSource.GROUP && activeMember && swappable && assigneeIsMe

    /**
     * Whether this user may cover the shift.
     *
     * Deliberately not `&& !isOwner`: if B offers back a shift they took from A, A may take it
     * again — `takeEvent` only refuses a taker who already holds it.
     */
    val canTake: Boolean
        get() = source == EventSource.GROUP && activeMember && onSwap && !assigneeIsMe

    /** The same event as the grid can draw it. */
    val cell: CalendarCellEventUi = CalendarCellEventUi(
        id = id,
        label = acronym?.takeIf { it.isNotBlank() } ?: name,
        background = background,
        onSwap = onSwap,
        assignedToOther = !assigneeIsMe && isOwner,
    )
}

fun GroupEvent.toUi(
    currentUserId: UserId?,
    removable: Boolean = false,
    activeMember: Boolean = false,
) = DayEventUi(
    id = id,
    groupId = groupId,
    ownerId = ownerId,
    assigneeId = assigneeId,
    source = EventSource.GROUP,
    name = type.name,
    acronym = type.acronym,
    date = date,
    background = colorHex.toComposeColorOrNull() ?: entityColor(type.id.value),
    onSwap = onSwap,
    swappable = type.swappable,
    activeMember = activeMember,
    isOwner = ownerId == currentUserId,
    assigneeName = assigneeName,
    assigneeIsMe = assigneeId == currentUserId,
    groupName = groupName,
    removable = removable,
    timeRange = type.hours(),
    transferChain = buildTransferChain(currentUserId),
)

/** Between the start and the end of [DayEventUi.timeRange]; `EventHours` splits on it to stack them. */
const val HOURS_SEPARATOR = " – "

/** Only clock times, which need no translating — so a lone start is shown bare, with no "from". */
private fun EventType.hours(): String? = when {
    startTime != null && endTime != null -> "$startTime$HOURS_SEPARATOR$endTime"
    else -> startTime
}

private fun GroupEvent.buildTransferChain(currentUserId: UserId?): List<TransferHolderUi> {
    if (history.size < 2) return emptyList()
    return history.map { entry ->
        TransferHolderUi(name = entry.userName, isMe = entry.userId == currentUserId)
    }
}

fun PersonalEvent.toUi(
    removable: Boolean = false,
    notesEditable: Boolean = false,
) = DayEventUi(
    id = id,
    groupId = null,
    ownerId = null,
    assigneeId = null,
    source = EventSource.PERSONAL,
    name = type.name,
    acronym = type.acronym,
    date = date.toLocalDate(),
    background = type.color.toComposeColorOrNull() ?: Color.Unspecified,
    timeRange = type.hours(),
    removable = removable,
    notes = notes,
    notesEditable = notesEditable,
)
