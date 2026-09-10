package com.geoviksoft.turnia.ui.main.swap.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.geoviksoft.turnia.core.domain.model.EventId
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.ui.components.calendar.model.DayEventUi
import com.geoviksoft.turnia.ui.components.calendar.model.EventSource
import com.geoviksoft.turnia.ui.components.calendar.model.TransferHolderUi
import com.geoviksoft.turnia.ui.system.PreviewTurniaTheme
import com.geoviksoft.turnia.ui.components.event.AcronymChip
import com.geoviksoft.turnia.ui.components.event.GroupLabel
import com.geoviksoft.turnia.ui.components.event.TransferTrail
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import kotlinx.datetime.LocalDate
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.calendar_months_short
import turnia.app.shared.generated.resources.calendar_weekdays_short
import turnia.app.shared.generated.resources.event_swap_take

/** Width of the date block, sized so "Mié" and a two-digit day both sit centred without wrapping. */
private val DateColumnWidth = 52.dp

/**
 * A shift in the swap tab.
 *
 * Not the day sheet's row: the lists here span three months, so **the date is what the reader is
 * looking for** and it leads, weekday included — "the tenth" is useless without knowing it is a
 * Thursday. What the day sheet says and this deliberately does not:
 *
 * - No "pidiendo cambio" badge. Every row under that tab is on offer; saying so on each one is noise.
 * - No "assigned to" chip. Where a shift has moved, [TransferTrail] already names who holds it and
 *   shows how it got there; where it has not, the holder is the reader.
 */
@Composable
fun SwapEventRow(
    event: DayEventUi,
    modifier: Modifier = Modifier,
    onTake: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(color = event.background, size = Size(6.dp.toPx(), size.height))
                }
                .padding(start = 14.dp, top = 12.dp, end = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DateBlock(event.date)

            Column(modifier = Modifier.weight(1f)) {
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

                event.groupName?.let { groupName ->
                    Spacer(Modifier.height(4.dp))
                    GroupLabel(name = groupName)
                }

                event.timeRange?.let { time ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = time,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                if (event.transferChain.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    TransferTrail(chain = event.transferChain)
                }

                if (onTake != null) {
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = onTake) {
                        Text(text = stringResource(Res.string.event_swap_take))
                    }
                }
            }
        }
    }
}

/** Weekday, day, month — stacked, because that is the order they are read in. */
@Composable
private fun DateBlock(date: LocalDate) {
    val weekdays = stringArrayResource(Res.array.calendar_weekdays_short)
    val months = stringArrayResource(Res.array.calendar_months_short)

    Column(
        modifier = Modifier.width(DateColumnWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = weekdays[date.dayOfWeek.ordinal],
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = date.day.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = months[date.month.ordinal],
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview
@Composable
private fun SwapEventRowPreview() {
    PreviewTurniaTheme {
        Column(
            modifier = Modifier.width(360.dp).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // "Los ofrezco": mine, still mine, no chain — so nothing but the shift and its date.
            SwapEventRow(event = previewEvent())
            // "Los cubre otro": one transfer, so the trail names who has it now.
            SwapEventRow(
                event = previewEvent(
                    chain = listOf(
                        TransferHolderUi("Yo", isMe = true),
                        TransferHolderUi("Bruno", isMe = false),
                    ),
                ),
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
                onTake = {},
            )
            // A shift with no acronym and no hours, to check the layout does not collapse.
            SwapEventRow(event = previewEvent(name = "Refuerzo", acronym = null, timeRange = null))
        }
    }
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
