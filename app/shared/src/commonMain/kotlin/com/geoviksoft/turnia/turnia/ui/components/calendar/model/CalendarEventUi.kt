package com.geoviksoft.turnia.ui.components.calendar.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.geoviksoft.turnia.core.domain.model.EventId
import com.geoviksoft.turnia.core.domain.model.GroupEvent
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.PersonalEvent
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.system.toLocalDate
import com.geoviksoft.turnia.ui.system.entityColor
import com.geoviksoft.turnia.ui.system.toComposeColorOrNull
import kotlinx.datetime.LocalDate

enum class EventSource { GROUP, PERSONAL }

data class TransferHolderUi(
    val name: String,
    val isMe: Boolean,
)

@Immutable
data class CalendarEventUi(
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
    val textColor: Color,
    val timeRange: String?,
    val subtitle: String,
    val onSwap: Boolean,
    val isOwner: Boolean,
    val assigneeName: String,
    val assigneeIsMe: Boolean,
    val groupName: String?,
    val transferChain: List<TransferHolderUi>,
    val removable: Boolean,
    val notes: String?,
    val notesEditable: Boolean,
) {
    val gridLabel: String get() = acronym?.takeIf { it.isNotBlank() } ?: name

    val assignedToOther: Boolean get() = !assigneeIsMe && isOwner

    companion object {
        fun create(
            id: EventId,
            groupId: GroupId?,
            ownerId: UserId?,
            assigneeId: UserId?,
            source: EventSource,
            name: String,
            background: Color,
            date: LocalDate,
            acronym: String? = null,
            timeRange: String? = null,
            subtitle: String = "",
            onSwap: Boolean = false,
            isOwner: Boolean = false,
            assigneeName: String = "",
            assigneeIsMe: Boolean = false,
            groupName: String? = null,
            transferChain: List<TransferHolderUi> = emptyList(),
            removable: Boolean = false,
            notes: String? = null,
            notesEditable: Boolean = false,
        ): CalendarEventUi = CalendarEventUi(
            id = id,
            groupId = groupId,
            ownerId = ownerId,
            assigneeId = assigneeId,
            source = source,
            name = name,
            acronym = acronym,
            background = background,
            textColor = if (background.luminance() > 0.5f) Color.Black else Color.White,
            timeRange = timeRange,
            subtitle = subtitle,
            onSwap = onSwap,
            isOwner = isOwner,
            assigneeName = assigneeName,
            assigneeIsMe = assigneeIsMe,
            groupName = groupName,
            date = date,
            transferChain = transferChain,
            removable = removable,
            notes = notes,
            notesEditable = notesEditable,
        )
    }
}


fun GroupEvent.toUi(
    currentUserId: UserId?,
    removable: Boolean = false,
) = CalendarEventUi.create(
    id = id,
    groupId = groupId,
    ownerId = ownerId,
    assigneeId = assigneeId,
    source = EventSource.GROUP,
    name = type.name,
    acronym = type.acronym,
    date = date,
    background = colorHex.toComposeColorOrNull() ?: entityColor(type.id.value),
    isOwner = ownerId == currentUserId,
    assigneeName = assigneeName,
    assigneeIsMe = assigneeId == currentUserId,
    groupName = groupName,
    removable = removable,
    transferChain = buildTransferChain(currentUserId),
)

private fun GroupEvent.buildTransferChain(currentUserId: UserId?): List<TransferHolderUi> {
    if (history.size < 2) return emptyList()
    return history.map { entry ->
        TransferHolderUi(name = entry.userName, isMe = entry.userId == currentUserId)
    }
}

fun PersonalEvent.toUi(
    removable: Boolean = false,
    notesEditable: Boolean = false,
) = CalendarEventUi.create(
    id = id,
    groupId = null,
    ownerId = null,
    assigneeId = null,
    source = EventSource.PERSONAL,
    name = type.name,
    acronym = type.acronym,
    date = date.toLocalDate(),
    background = type.color.toComposeColorOrNull() ?: Color.Unspecified,
    removable = removable,
    notes = notes,
    notesEditable = notesEditable,
)
