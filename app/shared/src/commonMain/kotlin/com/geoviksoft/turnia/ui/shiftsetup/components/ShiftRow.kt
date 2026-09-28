package com.geoviksoft.turnia.ui.shiftsetup.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.geoviksoft.turnia.ui.shiftsetup.model.ShiftPreset
import com.geoviksoft.turnia.ui.shiftsetup.model.ShiftRowUi
import com.geoviksoft.turnia.ui.system.PreviewTurniaTheme
import com.geoviksoft.turnia.ui.system.TestTags
import com.geoviksoft.turnia.ui.system.color.EntityPalette
import com.geoviksoft.turnia.ui.system.components.AcronymBadge
import com.geoviksoft.turnia.ui.system.components.time.TTimeField
import com.geoviksoft.turnia.ui.system.components.time.nextDayMark
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.event_type_field_end
import turnia.app.shared.generated.resources.event_type_field_start
import turnia.app.shared.generated.resources.shift_setup_edit_hours
import turnia.app.shared.generated.resources.shift_setup_no_times

@Composable
fun ShiftRow(
    index: Int,
    row: ShiftRowUi,
    name: String,
    acronym: String,
    onExpand: () -> Unit,
    onToggle: () -> Unit,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().testTag(TestTags.shiftSetupRow(index))) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Checkbox(checked = row.selected, onCheckedChange = { onToggle() })
            AcronymBadge(color = row.color, acronym = acronym, size = 36.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = row.schedule() ?: stringResource(Res.string.shift_setup_no_times),
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (row.selected) {
                IconButton(onClick = onExpand, modifier = Modifier.testTag(TestTags.shiftSetupExpand(index))) {
                    Icon(
                        imageVector = if (row.expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = stringResource(Res.string.shift_setup_edit_hours),
                    )
                }
            }
        }
        if (row.selected && row.expanded) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TTimeField(
                    value = row.start,
                    onValueChange = onStartChange,
                    label = stringResource(Res.string.event_type_field_start),
                    modifier = Modifier.weight(1f).testTag(TestTags.shiftSetupStart(index)),
                )
                TTimeField(
                    value = row.end,
                    onValueChange = onEndChange,
                    label = stringResource(Res.string.event_type_field_end),
                    modifier = Modifier.weight(1f).testTag(TestTags.shiftSetupEnd(index)),
                )
            }
        }
    }
}

/** "08:00 – 15:00", with "+1" when it ends the next day; null when the shift has no set hours. */
private fun ShiftRowUi.schedule(): String? {
    if (start.isBlank() || end.isBlank()) return start.ifBlank { null }
    return "$start – $end${nextDayMark(start, end)}"
}

@Preview
@Composable
fun ShiftRowPreview() {
    val row = ShiftRowUi(
        id = "id",
        preset = ShiftPreset.Afternoon,
        name = "name",
        acronym = "acronym",
        start = "15:00",
        end = "22:00",
        color = EntityPalette[0],
        selected = true
    )

    PreviewTurniaTheme {
        ShiftRow(
            index = 1,
            row = row,
            name = "Name",
            acronym = "N",
            onExpand = {},
            onToggle = {},
            onStartChange = {},
            onEndChange = {},
        )
    }
}

@Preview
@Composable
fun ShiftRowExpandedPreview() {
    val row = ShiftRowUi(
        id = "id",
        preset = ShiftPreset.Afternoon,
        name = "name",
        acronym = "acronym",
        start = "15:00",
        end = "22:00",
        color = EntityPalette[0],
        selected = true,
        expanded = true,
    )

    PreviewTurniaTheme {
        ShiftRow(
            index = 1,
            row = row,
            name = "Name",
            acronym = "N",
            onExpand = {},
            onToggle = {},
            onStartChange = {},
            onEndChange = {},
        )
    }
}
