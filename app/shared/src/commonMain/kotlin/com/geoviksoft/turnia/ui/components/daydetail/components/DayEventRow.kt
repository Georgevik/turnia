package com.geoviksoft.turnia.ui.components.daydetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.geoviksoft.turnia.ui.components.calendar.diagonalHatch
import com.geoviksoft.turnia.core.domain.model.EventId
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.ui.components.calendar.model.DayEventUi
import com.geoviksoft.turnia.ui.components.calendar.model.EventSource
import com.geoviksoft.turnia.ui.system.PreviewTurniaTheme
import kotlinx.datetime.LocalDate
import com.geoviksoft.turnia.ui.components.calendar.model.TransferHolderUi
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.event_assigned_to
import turnia.app.shared.generated.resources.event_holder_me
import turnia.app.shared.generated.resources.event_note_add
import turnia.app.shared.generated.resources.event_note_edit
import turnia.app.shared.generated.resources.event_remove
import turnia.app.shared.generated.resources.event_status_on_swap
import turnia.app.shared.generated.resources.event_swap_take
import turnia.app.shared.generated.resources.event_swap_toggle
import turnia.app.shared.generated.resources.group_member_former

@Composable
fun DayEventRow(
    event: DayEventUi,
    onRemove: (() -> Unit)? = null,
    onEditNotes: (() -> Unit)? = null,
    onSwapChange: ((Boolean) -> Unit)? = null,
    onTake: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 1.dp,
    ) {
        Box {
            if (event.assignedToOther) {
                Box(
                    Modifier
                        .matchParentSize()
                        .diagonalHatch(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                            strokeWidth = 2.dp,
                            spacing = 8.dp,
                        ),
                )
            }

            Row(modifier = Modifier.fillMaxWidth().drawBehind {
                drawRect(
                    color = event.background,
                    size = Size(6.dp.toPx(), size.height)
                )
            }) {
                Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        event.acronym?.takeIf { it.isNotBlank() }?.let {
                            AcronymChip(
                                acronym = it,
                                background = event.background,
                                textColor = event.textColor,
                            )
                        }
                        Text(
                            text = event.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (onRemove != null) {
                            IconButton(
                                onClick = onRemove,
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(Res.string.event_remove),
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    event.groupName?.let { groupName ->
                        Spacer(Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = groupName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // A group event always names who covers it. A blank name means the person
                    // was removed from the group: their shift stays, but they are no longer in
                    // the roster it is read from.
                    val isGroupEvent = event.source == EventSource.GROUP
                    val showAssignee =
                        event.assigneeIsMe || event.assigneeName.isNotBlank() || isGroupEvent
                    if (event.onSwap || showAssignee) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (event.onSwap) SwapChip()
                            if (showAssignee) {
                                val assignee = when {
                                    event.assigneeIsMe ->
                                        stringResource(Res.string.event_holder_me)

                                    event.assigneeName.isNotBlank() -> event.assigneeName
                                    else -> stringResource(Res.string.group_member_former)
                                }
                                AssignedToChip(name = assignee)
                            }
                        }
                    }

                    event.timeRange?.let { time ->
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = time,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    if (event.transferChain.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp).width(13.dp))
                        TransferTrail(chain = event.transferChain)
                    }

                    if (onSwapChange != null) {
                        Spacer(Modifier.height(4.dp))
                        SwapToggle(checked = event.onSwap, onCheckedChange = onSwapChange)
                    }

                    if (onTake != null) {
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = onTake) {
                            Text(text = stringResource(Res.string.event_swap_take))
                        }
                    }

                    // The note is shown whenever there is one, even somebody else's: on a shared
                    // calendar it belongs to the event's owner and is read, not edited.
                    val notes = event.notes?.takeIf { it.isNotBlank() }
                    if (notes != null) {
                        Spacer(Modifier.height(10.dp))
                        NoteBlock(notes = notes, onClick = onEditNotes)
                    } else if (onEditNotes != null) {
                        Spacer(Modifier.height(8.dp))
                        AddNoteButton(onClick = onEditNotes)
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteBlock(notes: String, onClick: (() -> Unit)?) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.StickyNote2,
                contentDescription = stringResource(Res.string.event_note_edit),
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = notes,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun AddNoteButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier.clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Default.NoteAdd,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(Res.string.event_note_add),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun TransferTrail(chain: List<TransferHolderUi>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        chain.forEachIndexed { index, holder ->
            if (index > 0) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp).align(Alignment.CenterVertically),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val name = when {
                holder.isMe -> stringResource(Res.string.event_holder_me)
                holder.name.isNotBlank() -> holder.name
                else -> stringResource(Res.string.group_member_former)
            }
            HolderPill(name = name, highlighted = index == chain.lastIndex)
        }
    }
}

@Composable
private fun HolderPill(name: String, highlighted: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (highlighted) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = if (highlighted) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        Text(
            text = name,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

@Composable
private fun AcronymChip(acronym: String, background: Color, textColor: Color) {
    val hasColor = background.isSpecified
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (hasColor) background else MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Text(
            text = acronym,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (hasColor) textColor else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Offering the shift, or taking the offer back. Only ever shown to whoever covers it. */
@Composable
private fun SwapToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.event_swap_toggle),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SwapChip() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = stringResource(Res.string.event_status_on_swap),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun AssignedToChip(name: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(Res.string.event_assigned_to, name),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// Previews. Split by what they exercise rather than crammed into one, because a row is tall and a
// dozen of them in a single preview is unreadable. Labels are developer-facing, so they stay here.

/** The width the row gets inside the day sheet, near enough to preview at. */
private val PreviewRowWidth = 340.dp

/** The shapes a row takes from the event itself, before any action is offered on it. */
@Preview
@Composable
fun DayEventRowPreview() {
    PreviewTurniaTheme {
        PreviewRows {
            Labelled("personal, with a note") {
                DayEventRow(
                    event = personalEvent(notes = "Cambiar con Marta si puede"),
                    onEditNotes = {},
                    onRemove = {},
                )
            }
            Labelled("personal, no note yet") {
                DayEventRow(event = personalEvent(), onEditNotes = {}, onRemove = {})
            }
            Labelled("group, I cover it") {
                DayEventRow(event = groupEvent(), onRemove = {})
            }
            // Hatched, and it keeps its place on the calendar: the shift is still this user's
            // doing even though somebody else works it now.
            Labelled("group, somebody else covers it") {
                DayEventRow(event = groupEvent(assigneeIsMe = false, assigneeName = "Bruno"))
            }
            Labelled("group, whoever covered it has left") {
                DayEventRow(event = groupEvent(assigneeIsMe = false, assigneeName = ""))
            }
            Labelled("no acronym, no times") {
                DayEventRow(event = groupEvent(acronym = null, timeRange = null))
            }
        }
    }
}

/** Every control the row can offer, and the badge it shows when it can offer none. */
@Preview
@Composable
fun DayEventRowSwapPreview() {
    PreviewTurniaTheme {
        PreviewRows {
            Labelled("mine, not offered — toggle off") {
                DayEventRow(event = groupEvent(), onSwapChange = {}, onRemove = {})
            }
            Labelled("mine, offered — toggle on") {
                DayEventRow(event = groupEvent(onSwap = true), onSwapChange = {}, onRemove = {})
            }
            // Somebody else's offer: a badge saying so, and a way to answer it.
            Labelled("somebody else's offer — I can cover it") {
                DayEventRow(
                    event = groupEvent(onSwap = true, assigneeIsMe = false, assigneeName = "Ana"),
                    onTake = {},
                )
            }
            // The read-only case: an offer on a colleague's shared calendar, or in a group this
            // user was removed from. The badge shows; nothing is actionable.
            Labelled("an offer I cannot answer") {
                DayEventRow(
                    event = groupEvent(
                        onSwap = true,
                        assigneeIsMe = false,
                        assigneeName = "Ana",
                        activeMember = false,
                    ),
                )
            }
        }
    }
}

/** The chain, which is the whole point of the app and only shows once a shift has moved twice. */
@Preview
@Composable
fun DayEventRowChainPreview() {
    PreviewTurniaTheme {
        PreviewRows {
            Labelled("handed on once") {
                DayEventRow(
                    event = groupEvent(
                        assigneeIsMe = false,
                        assigneeName = "Bruno",
                        transferChain = listOf(
                            TransferHolderUi("Ana", isMe = true),
                            TransferHolderUi("Bruno", isMe = false),
                        ),
                    ),
                )
            }
            Labelled("A → B → C, and I am the C") {
                DayEventRow(
                    event = groupEvent(
                        isOwner = false,
                        transferChain = listOf(
                            TransferHolderUi("Ana", isMe = false),
                            TransferHolderUi("Bruno", isMe = false),
                            TransferHolderUi("", isMe = true),
                        ),
                    ),
                    onSwapChange = {},
                )
            }
            Labelled("a link whose member has left the group") {
                DayEventRow(
                    event = groupEvent(
                        assigneeIsMe = false,
                        assigneeName = "Carla",
                        transferChain = listOf(
                            TransferHolderUi("Ana", isMe = false),
                            TransferHolderUi("", isMe = false),
                            TransferHolderUi("Carla", isMe = false),
                        ),
                    ),
                )
            }
        }
    }
}

@Composable
private fun PreviewRows(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .width(PreviewRowWidth)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun Labelled(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

private val previewMe = UserId("me")
private val previewDate = LocalDate(2026, 9, 10)

private fun groupEvent(
    acronym: String? = "M",
    timeRange: String? = "07:00 – 15:00",
    onSwap: Boolean = false,
    assigneeIsMe: Boolean = true,
    assigneeName: String = "",
    isOwner: Boolean = true,
    activeMember: Boolean = true,
    transferChain: List<TransferHolderUi> = emptyList(),
) = DayEventUi(
    id = EventId("preview-group-$acronym-$onSwap-$assigneeIsMe-$activeMember-${transferChain.size}"),
    groupId = GroupId("group"),
    ownerId = previewMe,
    assigneeId = previewMe,
    source = EventSource.GROUP,
    name = "Mañana",
    acronym = acronym,
    background = Color(0xFF4DB6AC),
    date = previewDate,
    timeRange = timeRange,
    onSwap = onSwap,
    swappable = true,
    activeMember = activeMember,
    isOwner = isOwner,
    assigneeName = assigneeName,
    assigneeIsMe = assigneeIsMe,
    groupName = "Urgencias",
    transferChain = transferChain,
)

private fun personalEvent(notes: String? = null) = DayEventUi(
    id = EventId("preview-personal-${notes != null}"),
    groupId = null,
    ownerId = null,
    assigneeId = null,
    source = EventSource.PERSONAL,
    name = "Dentista",
    acronym = null,
    background = Color(0xFFFFF176),
    date = previewDate,
    timeRange = "17:30 – 18:00",
    removable = true,
    notes = notes,
    notesEditable = true,
)
