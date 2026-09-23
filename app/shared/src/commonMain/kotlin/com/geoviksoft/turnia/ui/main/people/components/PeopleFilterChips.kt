package com.geoviksoft.turnia.ui.main.people.components

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
import com.geoviksoft.turnia.ui.main.people.model.PeopleFilter
import com.geoviksoft.turnia.ui.system.TurniaTheme
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.people_filter_hidden
import turnia.app.shared.generated.resources.people_filter_shared_by_me
import turnia.app.shared.generated.resources.people_filter_shared_with_me

@Composable
fun PeopleFilterChips(
    selected: PeopleFilter,
    hiddenCount: Int,
    onSelected: (PeopleFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PeopleFilter.entries
            .filter { it != PeopleFilter.HIDDEN || hiddenCount > 0 }
            .forEach { filter ->
                FilterChip(
                    selected = filter == selected,
                    onClick = { onSelected(filter) },
                    label = { Text(filter.label(hiddenCount)) },
                )
            }
    }
}

@Composable
private fun PeopleFilter.label(hiddenCount: Int): String = when (this) {
    PeopleFilter.SHARED_BY_ME -> stringResource(Res.string.people_filter_shared_by_me)
    PeopleFilter.SHARED_WITH_ME -> stringResource(Res.string.people_filter_shared_with_me)
    PeopleFilter.HIDDEN -> stringResource(Res.string.people_filter_hidden, hiddenCount)
}

@Preview
@Composable
private fun PeopleFilterChipsPreview() {
    TurniaTheme {
        PeopleFilterChips(selected = PeopleFilter.SHARED_BY_ME, hiddenCount = 2, onSelected = {})
    }
}
