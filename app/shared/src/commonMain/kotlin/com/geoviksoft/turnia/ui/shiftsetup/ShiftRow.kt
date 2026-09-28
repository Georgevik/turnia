package com.geoviksoft.turnia.ui.shiftsetup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.geoviksoft.turnia.ui.system.TestTags
import com.geoviksoft.turnia.ui.system.components.AcronymBadge
import com.geoviksoft.turnia.ui.system.components.time.TTimeField
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.event_type_field_end
import turnia.app.shared.generated.resources.event_type_field_start

@Composable
internal fun ShiftRow(
    index: Int,
    row: ShiftRowUi,
    name: String,
    acronym: String,
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
            Text(text = name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        }
        if (row.selected) {
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
