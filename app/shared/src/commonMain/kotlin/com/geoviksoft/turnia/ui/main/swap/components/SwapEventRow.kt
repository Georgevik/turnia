package com.geoviksoft.turnia.ui.main.swap.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geoviksoft.turnia.core.domain.model.EventId
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.domain.model.UserProfile
import com.geoviksoft.turnia.ui.components.calendar.model.DayEventUi
import com.geoviksoft.turnia.ui.components.calendar.model.EventSource
import com.geoviksoft.turnia.ui.components.calendar.model.TransferHolderUi
import com.geoviksoft.turnia.ui.components.event.AcronymChip
import com.geoviksoft.turnia.ui.components.event.GroupLabel
import com.geoviksoft.turnia.ui.components.event.TransferTrail
import com.geoviksoft.turnia.ui.main.swap.model.SwapRequesterUi
import com.geoviksoft.turnia.ui.system.PreviewTurniaTheme
import com.geoviksoft.turnia.ui.system.components.UserAvatar
import com.geoviksoft.turnia.ui.system.components.UserAvatarSize
import com.geoviksoft.turnia.ui.system.successColors
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.calendar_months_short
import turnia.app.shared.generated.resources.calendar_weekdays_short
import turnia.app.shared.generated.resources.event_swap_take
import turnia.app.shared.generated.resources.group_member_former
import turnia.app.shared.generated.resources.swap_covered_by
import turnia.app.shared.generated.resources.swap_covered_by_former
import turnia.app.shared.generated.resources.swap_date_today
import turnia.app.shared.generated.resources.swap_requested_by
import kotlin.time.Clock

/** Sized so a weekday, "Hoy" and a two-digit day all sit centred without wrapping. */
private val DateTileWidth = 52.dp
private val DateTileGap = 12.dp

/**
 * A shift in the swap tab.
 */
@Composable
fun SwapEventRow(
    event: DayEventUi,
    modifier: Modifier = Modifier,
    requestedBy: SwapRequesterUi? = null,
    coveredBy: String? = null,
    today: LocalDate = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) },
    onTake: (() -> Unit)? = null,
) {
    val cardColor = if (coveredBy != null) {
        MaterialTheme.successColors.container
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        shadowElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            requestedBy?.let { requester ->
                RequesterHeader(requester)
                Spacer(Modifier.height(10.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DateTileGap),
            ) {
                DateTile(date = event.date, today = today, tint = event.background, ground = cardColor)

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
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
                    }

                    MetaLine(groupName = event.groupName, timeRange = event.timeRange)

                    coveredBy?.let { CoveredLine(name = it) }
                }
            }

            // Indented to the text column, so the tile stays the only thing on the left edge.
            val contentStart = Modifier.padding(start = DateTileWidth + DateTileGap)

            // On a covered request a two-link chain is only "me → them", which the tick already says.
            val trailAddsSomething = coveredBy == null || event.transferChain.size > 2
            if (event.transferChain.isNotEmpty() && trailAddsSomething) {
                HorizontalDivider(
                    modifier = contentStart.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
                TransferTrail(chain = event.transferChain, modifier = contentStart)
            }

            if (onTake != null) {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onTake,
                    modifier = Modifier.align(Alignment.End),
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(text = stringResource(Res.string.event_swap_take))
                }
            }
        }
    }
}

@Composable
private fun RequesterHeader(requester: SwapRequesterUi) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        UserAvatar(avatar = requester.avatar, size = UserAvatarSize.S)
        Text(
            text = requester.name.ifBlank { stringResource(Res.string.group_member_former) },
            modifier = Modifier.weight(1f, fill = false),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(Res.string.swap_requested_by),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun CoveredLine(name: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.successColors.onContainer,
        )
        Text(
            text = if (name.isBlank()) {
                stringResource(Res.string.swap_covered_by_former)
            } else {
                stringResource(Res.string.swap_covered_by, name)
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.successColors.onContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Group and hours on one line: both are context, and neither is worth a line of its own. */
@Composable
private fun MetaLine(groupName: String?, timeRange: String?) {
    if (groupName == null && timeRange == null) return

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        groupName?.let { GroupLabel(name = it, modifier = Modifier.weight(1f, fill = false)) }
        timeRange?.let { time ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * Weekday, day, month — stacked, because that is the order they are read in. The month stays on the
 * tile even under the list's month heading: a row read on its own, without scrolling back up, has
 * to say which month it is. The tile takes the shift's colour, faintly, so the date and the acronym
 * beside it read as one thing.
 */
@Composable
private fun DateTile(date: LocalDate, today: LocalDate, tint: Color, ground: Color) {
    val weekdays = stringArrayResource(Res.array.calendar_weekdays_short)
    val months = stringArrayResource(Res.array.calendar_months_short)
    val colors = MaterialTheme.colorScheme

    val soon = date == today || date == today.plus(1, DateTimeUnit.DAY)
    val label = if (date == today) {
        stringResource(Res.string.swap_date_today)
    } else {
        weekdays[date.dayOfWeek.ordinal]
    }
    val background = when {
        soon -> colors.primaryContainer
        tint.isSpecified -> tint.copy(alpha = 0.14f).compositeOver(ground)
        else -> colors.surfaceContainerHigh
    }
    val content = if (soon) colors.onPrimaryContainer else colors.onSurface
    val secondary = if (soon) colors.onPrimaryContainer else colors.onSurfaceVariant

    Surface(
        modifier = Modifier.width(DateTileWidth),
        shape = RoundedCornerShape(12.dp),
        color = background,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = secondary,
                maxLines = 1,
            )
            Text(
                text = date.day.toString(),
                // Tighter than the style's own leading, so three lines cost little more than two.
                style = MaterialTheme.typography.titleLarge.copy(lineHeight = 24.sp),
                fontWeight = FontWeight.Bold,
                color = content,
            )
            Text(
                text = months[date.month.ordinal],
                style = MaterialTheme.typography.labelSmall,
                color = secondary,
                maxLines = 1,
            )
        }
    }
}

@Preview
@Composable
private fun SwapEventRowPreview() {
    val today = LocalDate(2026, 9, 9)
    PreviewTurniaTheme {
        Column(
            modifier = Modifier.width(360.dp).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // "Mis ofertas": mine, still mine, no chain — so nothing but the shift and its date.
            SwapEventRow(event = previewEvent(), today = today)
            // Tomorrow, so the tile is lit.
            SwapEventRow(event = previewEvent(date = LocalDate(2026, 9, 10)), today = today)
            // One I took from Ana and now need swapped again: the trail says where it came from.
            SwapEventRow(
                event = previewEvent(
                    chain = listOf(
                        TransferHolderUi("Ana", isMe = false),
                        TransferHolderUi("Yo", isMe = true),
                    ),
                ),
                today = today,
            )
            // Mine, taken by Bruno and passed on to Carla: green, and the trail shows the whole way.
            SwapEventRow(
                event = previewEvent(
                    name = "Noche",
                    acronym = "N",
                    date = LocalDate(2026, 10, 3),
                    chain = listOf(
                        TransferHolderUi("Yo", isMe = true),
                        TransferHolderUi("Bruno", isMe = false),
                        TransferHolderUi("Carla", isMe = false),
                    ),
                ).copy(assigneeIsMe = false, assigneeName = "Carla"),
                coveredBy = "Carla",
                today = today,
            )
            // "Los ofrecen otros": Carla's shift, so her name heads the card above the button.
            SwapEventRow(
                event = previewEvent(name = "Tarde", acronym = "T", date = LocalDate(2026, 9, 18))
                    .copy(assigneeIsMe = false, isOwner = false, assigneeName = "Carla"),
                requestedBy = SwapRequesterUi(name = "Carla", avatar = UserProfile.AnimalAvatar.PREVIEW),
                today = today,
                onTake = {},
            )
            // "Mis ofertas", taken by Bruno: green, with his name by the tick.
            SwapEventRow(
                event = previewEvent(date = LocalDate(2026, 9, 20))
                    .copy(assigneeIsMe = false, assigneeName = "Bruno"),
                coveredBy = "Bruno",
                today = today,
            )
            // A shift with no acronym and no hours, to check the layout does not collapse.
            SwapEventRow(
                event = previewEvent(name = "Refuerzo", acronym = null, timeRange = null),
                today = today,
            )
        }
    }
}

private fun previewEvent(
    name: String = "Mañana",
    acronym: String? = "M",
    timeRange: String? = "07:00 – 15:00",
    date: LocalDate = LocalDate(2026, 9, 14),
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
