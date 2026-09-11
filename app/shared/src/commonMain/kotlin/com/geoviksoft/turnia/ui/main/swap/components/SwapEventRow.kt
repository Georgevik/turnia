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
import com.geoviksoft.turnia.ui.components.calendar.model.DayEventUi
import com.geoviksoft.turnia.ui.components.calendar.model.EventSource
import com.geoviksoft.turnia.ui.components.calendar.model.TransferHolderUi
import com.geoviksoft.turnia.ui.components.event.AcronymChip
import com.geoviksoft.turnia.ui.components.event.GroupLabel
import com.geoviksoft.turnia.ui.components.event.TransferTrail
import com.geoviksoft.turnia.ui.system.PreviewTurniaTheme
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
import turnia.app.shared.generated.resources.swap_date_today
import turnia.app.shared.generated.resources.swap_date_tomorrow
import kotlin.time.Clock

/** Sized so "Mañana" and a two-digit day both sit centred without wrapping. */
private val DateTileWidth = 52.dp
private val DateTileGap = 12.dp

/**
 * A shift in the swap tab.
 *
 * Not the day sheet's row: the lists here span three months, so **the date is what the reader is
 * looking for** and it leads, weekday included — "the tenth" is useless without knowing it is a
 * Thursday. Today and tomorrow are named instead, and lit: a shift about to happen is the one
 * whose swap is urgent. What the day sheet says and this deliberately does not:
 *
 * - No "pidiendo cambio" badge. Every row under that tab is on offer; saying so on each one is noise.
 * - No "assigned to" chip. Where a shift has moved, [TransferTrail] already names who holds it and
 *   shows how it got there; where it has not, the holder is the reader.
 */
@Composable
fun SwapEventRow(
    event: DayEventUi,
    modifier: Modifier = Modifier,
    today: LocalDate = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) },
    onTake: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DateTileGap),
            ) {
                DateTile(date = event.date, today = today, tint = event.background)

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
                }
            }

            // Indented to the text column, so the tile stays the only thing on the left edge.
            val contentStart = Modifier.padding(start = DateTileWidth + DateTileGap)

            if (event.transferChain.isNotEmpty()) {
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
private fun DateTile(date: LocalDate, today: LocalDate, tint: Color) {
    val weekdays = stringArrayResource(Res.array.calendar_weekdays_short)
    val months = stringArrayResource(Res.array.calendar_months_short)
    val colors = MaterialTheme.colorScheme

    val soon = when (date) {
        today -> stringResource(Res.string.swap_date_today)
        today.plus(1, DateTimeUnit.DAY) -> stringResource(Res.string.swap_date_tomorrow)
        else -> null
    }
    val background = when {
        soon != null -> colors.primaryContainer
        tint.isSpecified -> tint.copy(alpha = 0.14f).compositeOver(colors.surfaceContainerLowest)
        else -> colors.surfaceContainerHigh
    }
    val content = if (soon != null) colors.onPrimaryContainer else colors.onSurface
    val secondary = if (soon != null) colors.onPrimaryContainer else colors.onSurfaceVariant

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
                text = soon ?: weekdays[date.dayOfWeek.ordinal],
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
            // "Los ofrezco": mine, still mine, no chain — so nothing but the shift and its date.
            SwapEventRow(event = previewEvent(), today = today)
            // Tomorrow, so the tile is lit.
            SwapEventRow(event = previewEvent(date = LocalDate(2026, 9, 10)), today = today)
            // "Los cubre otro": one transfer, so the trail names who has it now.
            SwapEventRow(
                event = previewEvent(
                    chain = listOf(
                        TransferHolderUi("Yo", isMe = true),
                        TransferHolderUi("Bruno", isMe = false),
                    ),
                ),
                today = today,
            )
            // The same, offered onward by Bruno — which is the one row here that can be acted on.
            SwapEventRow(
                event = previewEvent(
                    name = "Noche",
                    acronym = "N",
                    date = LocalDate(2026, 10, 3),
                    chain = listOf(
                        TransferHolderUi("Yo", isMe = true),
                        TransferHolderUi("Bruno", isMe = false),
                    ),
                ),
                today = today,
                onTake = {},
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
