package com.geoviksoft.turnia.ui.components.daydetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.geoviksoft.turnia.ui.system.TestTags
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.add_pane_create_shifts
import turnia.app.shared.generated.resources.add_pane_no_shifts

/** Nothing to reuse yet: shifts are made once, and from then on each one is a tap away. */
@Composable
internal fun NoShiftsPrompt(onCreate: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag(TestTags.ADD_PANE_EMPTY_SHIFTS),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(Res.string.add_pane_no_shifts),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onCreate, contentPadding = PaddingValues(0.dp)) {
                Text(stringResource(Res.string.add_pane_create_shifts))
            }
        }
    }
}
