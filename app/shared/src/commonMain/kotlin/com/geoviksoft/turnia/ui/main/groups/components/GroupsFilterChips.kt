package com.geoviksoft.turnia.ui.main.groups.components

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
import com.geoviksoft.turnia.ui.main.groups.model.GroupsFilter
import com.geoviksoft.turnia.ui.system.TurniaTheme
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.groups_filter_all
import turnia.app.shared.generated.resources.groups_filter_mine
import turnia.app.shared.generated.resources.groups_filter_pending

@Composable
fun GroupsFilterChips(
    selected: GroupsFilter,
    pendingCount: Int,
    onSelected: (GroupsFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GroupsFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelected(filter) },
                label = { Text(filter.label(pendingCount)) },
            )
        }
    }
}

@Composable
private fun GroupsFilter.label(pendingCount: Int): String = when (this) {
    GroupsFilter.ALL -> stringResource(Res.string.groups_filter_all)
    GroupsFilter.MINE -> stringResource(Res.string.groups_filter_mine)
    GroupsFilter.PENDING -> stringResource(Res.string.groups_filter_pending, pendingCount)
}

@Preview
@Composable
private fun GroupsFilterChipsPreview() {
    TurniaTheme {
        GroupsFilterChips(selected = GroupsFilter.ALL, pendingCount = 2, onSelected = {})
    }
}
