package com.georgevik.turnia.ui.main.people.components

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
import com.georgevik.turnia.ui.main.people.model.PeopleFilter
import com.georgevik.turnia.ui.system.TurniaTheme
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.people_filter_shared_by_me
import turnia.app.shared.generated.resources.people_filter_shared_with_me

@Composable
fun PeopleFilterChips(
    selected: PeopleFilter,
    onSelected: (PeopleFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PeopleFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelected(filter) },
                label = { Text(filter.label()) },
            )
        }
    }
}

@Composable
private fun PeopleFilter.label(): String = stringResource(
    when (this) {
        PeopleFilter.SHARED_BY_ME -> Res.string.people_filter_shared_by_me
        PeopleFilter.SHARED_WITH_ME -> Res.string.people_filter_shared_with_me
    }
)

@Preview
@Composable
private fun PeopleFilterChipsPreview() {
    TurniaTheme {
        PeopleFilterChips(selected = PeopleFilter.SHARED_BY_ME, onSelected = {})
    }
}
