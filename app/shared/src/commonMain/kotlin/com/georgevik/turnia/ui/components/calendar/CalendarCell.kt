package com.georgevik.turnia.ui.components.calendar

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.georgevik.turnia.ui.components.calendar.animtransition.CalendarSheetBoundsTransform
import com.georgevik.turnia.ui.components.calendar.model.CalendarEventUi
import kotlinx.datetime.LocalDate

/** Height reserved at the top of a cell for the day number. */
private val CalendarCellNumberHeight = 24.dp

/** Diameter of the "today" highlight circle behind the day number. */
private val CalendarDayNumberCircle = 24.dp

/** Margin between adjacent tiles (applied on all sides of each cell). */
private val CellOuterMargin = 2.dp

/** Inner padding between the tile edge and its content. */
private val CellContentPadding = 2.dp


/**
 * Shared-element keys used to morph a calendar tile into the day details sheet.
 * The cell and the sheet register the same key for the same date, so the
 * transition framework can match and animate between them.
 */
internal fun calendarContainerKey(date: LocalDate): String = "calendar-container-$date"
internal fun calendarNumberKey(date: LocalDate): String = "calendar-number-$date"

@OptIn(ExperimentalSharedTransitionApi::class)
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
    // Expand animation via SharedTransition
    sharedScope: SharedTransitionScope? = null,
    isExpanded: Boolean = false,
) {
    // Shared modifier for the tile background (the card that grows into the sheet).
    val containerModifier = getShareModifier(
        sharedTransitionScope = sharedScope,
        key = calendarContainerKey(date),
        visible = !isExpanded,
    )
    // Shared modifier for the day number (travels to the sheet header).
    val numberModifier = getShareModifier(
        sharedTransitionScope = sharedScope,
        key = calendarNumberKey(date),
        visible = !isExpanded,
        skipToLookaheadSize = true,
    )

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
            .then(containerModifier)
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
            // Day number, centered at the top.
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
                        modifier = numberModifier,
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
                        EventRow(modifier = Modifier.weight(1f), event = events.first())
                        Spacer(modifier.weight(1f))
                    }
                }

                events.size > 1 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        EventRow(modifier = Modifier.weight(1f), event = events[0])
                        EventRow(modifier = Modifier.weight(1f), event = events[1])
                        if (events.size > 2) {
                            OverflowRow()
                        }

                    }
                }
            }
        }
    }
}

/**
 *
 * @param visible whether this instance is the currently visible one; flip it to
 *   false to hand the transition off to the matching element in the open sheet.
 * @param skipToLookaheadSize keeps text/content laid out at its final size during
 *   the morph instead of reflowing — use it for the day number.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun getShareModifier(
    sharedTransitionScope: SharedTransitionScope?,
    key: String,
    visible: Boolean,
    skipToLookaheadSize: Boolean = false
): Modifier {
    if (sharedTransitionScope == null) return Modifier

    return with(sharedTransitionScope) {
        Modifier.sharedElementWithCallerManagedVisibility(
            sharedContentState = rememberSharedContentState(key),
            visible = visible,
            boundsTransform = CalendarSheetBoundsTransform,
        ).then(if (skipToLookaheadSize) Modifier.skipToLookaheadSize() else Modifier)
    }
}

@Composable
private fun EventRow(event: CalendarEventUi, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(event.background)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Owned by me but performed by someone else — hatch it (stripes under text).
        if (event.assignedToOther) {
            Box(Modifier.matchParentSize().diagonalHatch(event.textColor.copy(alpha = 0.65f)))
        }
        // Grow the label to fill the tiny tile so short siglas stay big and legible.
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
