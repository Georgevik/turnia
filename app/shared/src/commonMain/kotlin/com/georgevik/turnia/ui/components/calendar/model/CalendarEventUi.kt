package com.georgevik.turnia.ui.components.calendar.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.georgevik.turnia.core.domain.model.EventId
import com.georgevik.turnia.core.domain.model.GroupEvent
import com.georgevik.turnia.core.domain.model.PersonalEvent
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.system.toLocalDate
import com.georgevik.turnia.ui.system.toComposeColorOrNull
import kotlinx.datetime.LocalDate

enum class EventSource { GROUP, PERSONAL }

data class TransferHolderUi(
    val name: String,
    val isMe: Boolean,
)

@Immutable
data class CalendarEventUi(
    val id: EventId,
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
) {
    val gridLabel: String get() = acronym?.takeIf { it.isNotBlank() } ?: name

    val assignedToOther: Boolean get() = !assigneeIsMe && isOwner

    companion object {
        fun create(
            id: EventId,
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
        ): CalendarEventUi = CalendarEventUi(
            id = id,
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
        )
    }
}


fun GroupEvent.toUi(
    currentUserId: UserId?,
    removable: Boolean = false,
) = CalendarEventUi.create(
    id = id,
    source = EventSource.GROUP,
    name = type.name,
    acronym = type.acronym,
    date = date,
    background = colorHex.toComposeColorOrNull() ?: Color.Unspecified,
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

fun PersonalEvent.toUi(removable: Boolean = false) = CalendarEventUi.create(
    id = id,
    source = EventSource.PERSONAL,
    name = type.name,
    acronym = type.acronym,
    date = date.toLocalDate(),
    background = type.color.toComposeColorOrNull() ?: Color.Unspecified,
    removable = removable,
)
