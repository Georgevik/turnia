package com.geoviksoft.turnia.ui.components.event

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import com.geoviksoft.turnia.ui.components.calendar.model.HOURS_SEPARATOR

/** Past this share of the row, one line would squeeze the shift's name out of sight. */
private const val MAX_SINGLE_LINE_FRACTION = 0.45f

/**
 * A shift's hours, right-aligned at the end of its title row.
 *
 * Tabular digits so the times of consecutive cards line up. On one line while it leaves the name
 * room; otherwise the start sits over the end rather than cutting either.
 */
@Composable
fun EventHours(timeRange: String, modifier: Modifier = Modifier) {
    val style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum")
    val color = MaterialTheme.colorScheme.onSurface
    val parts = timeRange.split(HOURS_SEPARATOR)

    Layout(
        modifier = modifier,
        content = {
            Text(text = timeRange, style = style, color = color, maxLines = 1, softWrap = false)
            Column(horizontalAlignment = Alignment.End) {
                parts.forEach { part ->
                    Text(text = part, style = style, color = color, textAlign = TextAlign.End, maxLines = 1)
                }
            }
        },
    ) { measurables, constraints ->
        val singleLine = measurables[0].measure(Constraints())
        val fits = parts.size == 1 ||
            singleLine.width <= constraints.maxWidth * MAX_SINGLE_LINE_FRACTION
        val shown = if (fits) singleLine else measurables[1].measure(Constraints())
        layout(shown.width, shown.height) { shown.place(0, 0) }
    }
}
