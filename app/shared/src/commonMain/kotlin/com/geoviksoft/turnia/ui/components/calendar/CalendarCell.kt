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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geoviksoft.turnia.core.domain.model.EventId
import com.geoviksoft.turnia.ui.components.calendar.model.CalendarCellEventUi
import com.geoviksoft.turnia.ui.system.PreviewTurniaTheme
import kotlinx.datetime.LocalDate

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
    events: List<CalendarCellEventUi> = emptyList(),
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
    event: CalendarCellEventUi,
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
            text = event.label,
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

// Previews. The labels are developer-facing, so they stay here rather than in composeResources.

/** The tile size the month grid gives a cell on a phone, near enough to preview at. */
private val PreviewCellSize = DpSize(56.dp, 92.dp)

private val today = LocalDate(2026, 9, 10)

/** The chrome around the events: whether the day is today, chosen, or spilling in from next month. */
@Preview
@Composable
fun CalendarCellPreview() {
    PreviewTurniaTheme {
        Row {
            LabelledCell("today", isToday = true, events = listOf(demoCell()))
            LabelledCell("selected", isSelected = true, events = listOf(demoCell()))
            LabelledCell("in month", inMonth = true, events = listOf(demoCell()))
            LabelledCell("other month", events = listOf(demoCell()))
        }
    }
}

/**
 * Every state a tile can be in, which is the whole of what [CalendarCellEventUi] decides: the label,
 * the colour it is painted, whether it is hatched, whether the corner marker turns, and how many of
 * them fit before the cell gives up and shows an ellipsis.
 *
 * The marker is a still frame here — a preview does not run the animation — so the row it is in only
 * shows that it is *there* and in the corner, not that it turns.
 */
@Preview
@Composable
fun CalendarCellEventPreview() {
    PreviewTurniaTheme {
        Column {
            Row {
                LabelledCell("plain", events = listOf(demoCell()))
                LabelledCell("on swap", events = listOf(demoCell(onSwap = true)))
                LabelledCell("covered by other", events = listOf(demoCell(assignedToOther = true)))
                LabelledCell(
                    label = "both",
                    events = listOf(demoCell(onSwap = true, assignedToOther = true)),
                )
            }
            Row {
                // The text colour is derived from the background, so a user's colour choice can
                // never leave a tile unreadable. These four are the extremes of that.
                LabelledCell("light bg", events = listOf(demoCell(background = Color(0xFFFFF176))))
                LabelledCell("dark bg", events = listOf(demoCell(background = Color(0xFF1A237E))))
                LabelledCell("no acronym", events = listOf(demoCell(label = "Guardia")))
                LabelledCell("long label", events = listOf(demoCell(label = "REFUERZO")))
            }
            Row {
                // A cell draws two and hides the rest, which is why `swapFirst` exists: the third
                // one here is the one nobody would see.
                LabelledCell("one", events = listOf(demoCell()))
                LabelledCell("two", events = List(2) { demoCell(label = "T$it") })
                LabelledCell(
                    label = "three",
                    events = List(3) { demoCell(label = "T$it") },
                )
                LabelledCell("empty", events = emptyList())
            }
        }
    }
}

@Composable
private fun LabelledCell(
    label: String,
    events: List<CalendarCellEventUi>,
    inMonth: Boolean = false,
    isToday: Boolean = false,
    isSelected: Boolean = false,
) {
    Column(
        modifier = Modifier.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(PreviewCellSize)) {
            CalendarCell(
                date = today,
                inMonth = inMonth,
                isToday = isToday,
                isSelected = isSelected,
                theme = CalendarThemes.myCalendar(),
                onClick = {},
                events = events,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun demoCell(
    label: String = "DE",
    background: Color = Color(0xFF4DB6AC),
    onSwap: Boolean = false,
    assignedToOther: Boolean = false,
) = CalendarCellEventUi(
    // Only the animation's debug label reads it, so a repeat across previews costs nothing.
    id = EventId("preview-$label"),
    label = label,
    background = background,
    onSwap = onSwap,
    assignedToOther = assignedToOther,
)
