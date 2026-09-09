package com.geoviksoft.turnia.ui.main.swap.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.ui.main.swap.model.SwapGroupFilterUi
import com.geoviksoft.turnia.ui.system.components.TListItem
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.swap_filter_all
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
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item {
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = stringResource(Res.string.swap_filter_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        items(groups, key = { it.id.value }) { group ->
            TListItem(
                title = group.name,
                onClick = { onToggle(group.id) },
                leading = {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(group.color))
                },
                trailing = {
                    Checkbox(checked = group.selected, onCheckedChange = { onToggle(group.id) })
                },
            )
        }

        item {
            TextButton(
                onClick = onSelectAll,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(stringResource(Res.string.swap_filter_all))
            }
        }
    }
}
