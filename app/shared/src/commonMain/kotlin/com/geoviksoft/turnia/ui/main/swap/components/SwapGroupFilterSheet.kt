package com.geoviksoft.turnia.ui.main.swap.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.ui.main.swap.model.SwapGroupFilterUi
import com.geoviksoft.turnia.ui.system.PreviewTurniaTheme
import com.geoviksoft.turnia.ui.system.components.TListItem
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.swap_filter_all
import turnia.app.shared.generated.resources.swap_filter_subtitle
import turnia.app.shared.generated.resources.swap_filter_title

/**
 * Which groups the lists are drawn from. Everything is ticked until the user says otherwise, so the
 * filter starts out invisible in its effect: it only ever narrows.
 */
@Composable
fun SwapGroupFilterSheet(
    groups: List<SwapGroupFilterUi>,
    onToggle: (GroupId) -> Unit,
    onSelectAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(Res.string.swap_filter_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(Res.string.swap_filter_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))

        // A plain Column and not a LazyColumn: nobody belongs to enough groups for it to matter, and
        // a lazy list inside a sheet that sizes itself to its content fights the sheet.
        groups.forEach { group ->
            TListItem(
                title = group.name,
                onClick = { onToggle(group.id) },
                leading = { GroupDot(group.color) },
                trailing = {
                    Checkbox(checked = group.selected, onCheckedChange = { onToggle(group.id) })
                },
            )
        }

        if (groups.any { !it.selected }) {
            TextButton(onClick = onSelectAll) {
                Text(stringResource(Res.string.swap_filter_all))
            }
        }
    }
}

@Composable
private fun GroupDot(color: Color) {
    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(14.dp).clip(CircleShape).background(color))
    }
}

@Preview
@Composable
private fun SwapGroupFilterSheetPreview() {
    PreviewTurniaTheme {
        SwapGroupFilterSheet(
            groups = listOf(
                SwapGroupFilterUi(GroupId("a"), "Urgencias", Color(0xFF4DB6AC), selected = true),
                SwapGroupFilterUi(GroupId("b"), "Planta 3", Color(0xFFFFB74D), selected = false),
                SwapGroupFilterUi(GroupId("c"), "Quirófano", Color(0xFF9575CD), selected = true),
            ),
            onToggle = {},
            onSelectAll = {},
        )
    }
}
