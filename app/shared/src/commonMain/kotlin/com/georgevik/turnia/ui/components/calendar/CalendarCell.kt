package com.georgevik.turnia.ui.components.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate

@Composable
fun CalendarCell(
    date: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    theme: CalendarTheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.Companion,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
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
    }
}
