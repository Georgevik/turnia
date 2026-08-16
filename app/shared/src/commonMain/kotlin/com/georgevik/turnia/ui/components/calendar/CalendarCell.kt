package com.georgevik.turnia.ui.components.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate

/** Height of a single event row (and of the "•••" overflow indicator). */
internal val CalendarEventSlotHeight = 16.dp

/** Height reserved at the top of a cell for the day number. */
internal val CalendarCellNumberHeight = 30.dp

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
    maxEventRows: Int = 0,
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
            .fillMaxSize()
            .padding(2.dp),
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
                .padding(horizontal = 3.dp, vertical = 3.dp),
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
                        .size(28.dp)
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

            // Show as many event rows as fit ([maxEventRows], computed once from
            // the cell size); if there are more, the last slot becomes "•••".
            if (events.isNotEmpty() && maxEventRows > 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    if (events.size <= maxEventRows) {
                        events.forEach { EventRow(it) }
                    } else {
                        events.take(maxEventRows - 1).forEach { EventRow(it) }
                        OverflowRow()
                    }
                }
            }
        }
    }
}

@Composable
private fun EventRow(event: CalendarEventUi) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(CalendarEventSlotHeight)
            .padding(vertical = 1.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(event.background)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = event.text,
            style = MaterialTheme.typography.labelSmall,
            color = event.textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun OverflowRow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(CalendarEventSlotHeight),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "•••",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
