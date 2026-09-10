package com.geoviksoft.turnia.ui.components.calendar

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geoviksoft.turnia.core.domain.model.EventId
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.ui.components.calendar.model.CalendarEventUi
import com.geoviksoft.turnia.ui.components.calendar.model.EventSource
import com.geoviksoft.turnia.ui.system.PreviewTurniaTheme
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.random.Random
import kotlin.time.Clock

/** Height reserved at the top of a cell for the day number. */
private val CalendarCellNumberHeight = 24.dp

/** Diameter of the "today" highlight circle behind the day number. */
private val CalendarDayNumberCircle = 24.dp

/** Margin between adjacent tiles (applied on all sides of each cell). */
private val CellOuterMargin = 2.dp

/** Inner padding between the tile edge and its content. */
private val CellContentPadding = 2.dp

private const val SWAP_MARKER_SPIN_MS = 2200

@Composable
fun CalendarCell(
    date: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    theme: CalendarTheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.Companion,
    events: List<CalendarEventUi> = emptyList(),
) {
    val indicatorColor = if (isToday) theme.accentColor else Color.Transparent
    val numberColor = when {
        isToday -> contentColorFor(theme.accentColor)
        isSelected -> contentColorFor(theme.selectedBackground)
        inMonth -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .padding(CellOuterMargin)
            .fillMaxSize(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                theme.selectedBackground
            } else {
                MaterialTheme.colorScheme.surfaceContainerLowest
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = if (isSelected) BorderStroke(2.dp, theme.accentColor) else null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 3.dp, vertical = CellContentPadding),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CalendarCellNumberHeight),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(CalendarDayNumberCircle)
                        .clip(CircleShape)
                        .background(indicatorColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = date.day.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (inMonth || isToday) FontWeight.SemiBold else FontWeight.Normal,
                        color = numberColor,
                    )
                }
            }

            when {
                events.isEmpty() -> Unit
                events.size == 1 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        EventRow(
                            modifier = Modifier.weight(1f),
                            event = events.first(),
                        )
                        Spacer(modifier.weight(1f))
                    }
                }

                events.size > 1 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        EventRow(
                            modifier = Modifier.weight(1f),
                            event = events[0],
                        )
                        EventRow(
                            modifier = Modifier.weight(1f),
                            event = events[1],
                        )
                        if (events.size > 2) {
                            OverflowRow()
                        }

                    }
                }
            }
        }
    }
}

@Composable
private fun EventRow(
    event: CalendarEventUi,
    modifier: Modifier = Modifier,
) {
    val swapMarkerAngle by rememberInfiniteTransition(label = "swapMarker_${event.id}").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(SWAP_MARKER_SPIN_MS, easing = LinearEasing),
        ),
        label = "swapMarkerAngle",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(event.background)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (event.assignedToOther) {
            Box(Modifier.matchParentSize().diagonalHatch(event.textColor.copy(alpha = 0.65f)))
        }

        BasicText(
            text = event.gridLabel,
            maxLines = 1,
            style = TextStyle(
                color = event.textColor,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            ),
            autoSize = TextAutoSize.StepBased(
                minFontSize = 10.sp,
                maxFontSize = 20.sp,
                stepSize = 1.sp,
            ),
        )

        if (event.onSwap) {
            Icon(
                imageVector = Icons.Default.Autorenew,
                contentDescription = null,
                tint = event.textColor,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(11.dp)
                    .graphicsLayer { rotationZ = swapMarkerAngle },
            )
        }
    }
}

@Composable
private fun OverflowRow() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "•••",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview
@Composable
fun CalendarCellPreview() {
    PreviewTurniaTheme {
        Row {
            Box(modifier = Modifier.size(60.dp, 100.dp)) {
                CalendarCell(
                    Clock.System.todayIn(TimeZone.currentSystemDefault()),
                    true,
                    true,
                    false,
                    CalendarThemes.myCalendar(),
                    {},
                    events = listOf(demo_event.copy(assigneeId = demo_event.ownerId))
                )
            }
            Box(modifier = Modifier.size(60.dp, 100.dp)) {
                CalendarCell(
                    Clock.System.todayIn(TimeZone.currentSystemDefault()),
                    true,
                    false,
                    true,
                    CalendarThemes.myCalendar(),
                    {},
                    events = listOf(demo_event.copy(assigneeId = demo_event.ownerId))
                )
            }
            Box(modifier = Modifier.size(60.dp, 100.dp)) {
                CalendarCell(
                    Clock.System.todayIn(TimeZone.currentSystemDefault()),
                    false,
                    false,
                    false,
                    CalendarThemes.myCalendar(),
                    {},
                    events = listOf(demo_event.copy(assigneeId = demo_event.ownerId))
                )
            }
        }
    }
}

@Preview
@Composable
fun CalendarCellEventPreview() {
    PreviewTurniaTheme {
        Column {
            Row {
                CalendarCellPreviewDemo(demo_event.copy(assigneeIsMe = false))
                CalendarCellPreviewDemo(demo_event.copy(assigneeIsMe = true))
            }
            Row {
                CalendarCellPreviewDemo(demo_event.copy(onSwap = false))
                CalendarCellPreviewDemo(demo_event.copy(onSwap = true))
            }
        }

    }
}

@Composable
private fun CalendarCellPreviewDemo(vararg events: CalendarEventUi) {
    Box(modifier = Modifier.size(60.dp, 100.dp)) {
        CalendarCell(
            Clock.System.todayIn(TimeZone.currentSystemDefault()),
            false,
            false,
            false,
            CalendarThemes.myCalendar(),
            {},
            events = events.toList()
        )
    }
}

private val demo_event = CalendarEventUi(
    id = EventId(Random.nextInt().toString()),
    groupId = null,
    ownerId = UserId(Random.nextInt().toString()),
    assigneeId = UserId(Random.nextInt().toString()),
    source = EventSource.GROUP,
    name = "Demo Event",
    acronym = "DE",
    background = Color.Yellow,
    date = LocalDate(2023, 1, 1),
    textColor = Color.Black,
    timeRange = null,
    subtitle = "",
    onSwap = false,
    swappable = true,
    activeMember = true,
    isOwner = true,
    assigneeName = "Ricardo",
    assigneeIsMe = true,
    groupName = "MyGroup",
    transferChain = emptyList(),
    removable = false,
    notes = null,
    notesEditable = false,
)
