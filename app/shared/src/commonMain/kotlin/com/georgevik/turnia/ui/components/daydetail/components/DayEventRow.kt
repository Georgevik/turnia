package com.georgevik.turnia.ui.components.daydetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import com.georgevik.turnia.ui.components.calendar.diagonalHatch
import com.georgevik.turnia.ui.components.calendar.model.CalendarEventUi
import com.georgevik.turnia.ui.components.calendar.model.TransferHolderUi
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.event_assigned_to
import turnia.app.shared.generated.resources.event_holder_me
import turnia.app.shared.generated.resources.event_remove
import turnia.app.shared.generated.resources.event_status_on_swap

@Composable
fun DayEventRow(
    event: CalendarEventUi,
    onRemove: (() -> Unit)? = null,
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

                    val showAssignee = event.assigneeIsMe || event.assigneeName.isNotBlank()
                    if (event.onSwap || showAssignee) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (event.onSwap) SwapChip()
                            if (showAssignee) {
                                val assignee = if (event.assigneeIsMe) {
                                    stringResource(Res.string.event_holder_me)
                                } else {
                                    event.assigneeName
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
                }
            }
        }
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
            val name = if (holder.isMe) stringResource(Res.string.event_holder_me) else holder.name
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
