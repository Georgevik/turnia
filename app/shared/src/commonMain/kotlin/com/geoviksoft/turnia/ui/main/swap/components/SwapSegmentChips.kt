package com.geoviksoft.turnia.ui.main.swap.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.geoviksoft.turnia.ui.main.swap.model.SwapSegment
import com.geoviksoft.turnia.ui.system.PreviewTurniaTheme
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.swap_segment_colleagues
import turnia.app.shared.generated.resources.swap_segment_mine

/** Which of the lists is showing. Chips rather than tabs, as everywhere else that filters a list. */
@Composable
fun SwapSegmentChips(
    selected: SwapSegment,
    onSelected: (SwapSegment) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SwapSegment.entries.forEach { segment ->
            FilterChip(
                selected = segment == selected,
                onClick = { onSelected(segment) },
                label = { Text(segment.label()) },
            )
        }
    }
}

@Composable
private fun SwapSegment.label(): String = when (this) {
    SwapSegment.MINE -> stringResource(Res.string.swap_segment_mine)
    SwapSegment.COLLEAGUES -> stringResource(Res.string.swap_segment_colleagues)
}

@Preview
@Composable
private fun SwapSegmentChipsPreview() {
    PreviewTurniaTheme {
        SwapSegmentChips(selected = SwapSegment.MINE, onSelected = {})
    }
}
