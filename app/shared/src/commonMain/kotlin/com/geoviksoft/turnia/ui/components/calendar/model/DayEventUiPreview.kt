package com.geoviksoft.turnia.ui.components.calendar.model

import androidx.compose.ui.graphics.Color
import com.geoviksoft.turnia.core.domain.model.EventId
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.UserId
import kotlinx.datetime.LocalDate

object DayEventUiPreview {

    val A_DAY_EVENT = previewEvent()

}

private fun previewEvent(
    name: String = "Mañana",
    acronym: String? = "M",
    timeRange: String? = "07:00 – 15:00",
    date: LocalDate = LocalDate(2026, 9, 10),
    chain: List<TransferHolderUi> = emptyList(),
) = DayEventUi(
    id = EventId("preview-$name-$date"),
    groupId = GroupId("group"),
    ownerId = UserId("me"),
    assigneeId = UserId("me"),
    source = EventSource.GROUP,
    name = name,
    acronym = acronym,
    background = Color(0xFF4DB6AC),
    date = date,
    timeRange = timeRange,
    onSwap = true,
    swappable = true,
    activeMember = true,
    isOwner = true,
    assigneeIsMe = true,
    groupName = "Urgencias",
    transferChain = chain,
)
