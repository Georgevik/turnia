package com.geoviksoft.turnia.ui.components.daydetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.geoviksoft.turnia.ui.components.daydetail.model.OneOffEventUi
import com.geoviksoft.turnia.ui.system.PreviewTurniaTheme
import com.geoviksoft.turnia.ui.system.color.EntityPalette
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.one_off_event_all_day

/**
 * Laid out as an agenda line rather than like a shift's row: what a one-off event is known by is
 * its time, so the time leads, and there is no acronym, assignee or chain to show.
 */
@Composable
fun OneOffEventRow(
    event: OneOffEventUi,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min).padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.padding(start = 14.dp).width(64.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = if (event.allDay) {
                        stringResource(Res.string.one_off_event_all_day)
                    } else {
                        event.start.time.clockLabel()
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val endLabel = when {
                    !event.allDay -> event.end.endLabel(event.start.date)
                    event.end.date != event.start.date -> "→ ${event.end.date.dateLabel()}"
                    else -> null
                }
                endLabel?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(event.color, RoundedCornerShape(2.dp)),
            )
            Column(
                modifier = Modifier.weight(1f).padding(end = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                event.notes?.let { notes ->
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** An end on another day carries that day, or it reads as earlier than the start. */
private fun LocalDateTime.endLabel(startDate: LocalDate): String =
    if (date == startDate) time.clockLabel()
    else "${date.day}/${date.month.ordinal + 1} ${time.clockLabel()}"

// Previews. The values are developer-facing, so they stay here rather than in composeResources.

@Preview
@Composable
fun OneOffEventRowPreview() {
    val day = LocalDate(2026, 10, 3)
    PreviewTurniaTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OneOffEventRow(
                OneOffEventUi(
                    id = "dentist",
                    name = "Dentista",
                    notes = "Clínica Sonrisa, c/ Mayor 12. Llevar la tarjeta del seguro.",
                    start = LocalDateTime(day, LocalTime(17, 30)),
                    end = LocalDateTime(day, LocalTime(18, 15)),
                    allDay = false,
                    color = EntityPalette.first(),
                ),
            )
            OneOffEventRow(
                OneOffEventUi(
                    id = "congress",
                    name = "Congreso de enfermería",
                    notes = null,
                    start = LocalDateTime(day, LocalTime(9, 0)),
                    end = LocalDateTime(LocalDate(2026, 10, 4), LocalTime(14, 0)),
                    allDay = false,
                    color = EntityPalette[3],
                ),
            )
            OneOffEventRow(
                OneOffEventUi(
                    id = "holiday",
                    name = "Puente del Pilar",
                    notes = null,
                    start = LocalDateTime(day, LocalTime(9, 0)),
                    end = LocalDateTime(LocalDate(2026, 10, 5), LocalTime(10, 0)),
                    allDay = true,
                    color = EntityPalette[5],
                ),
            )
        }
    }
}
