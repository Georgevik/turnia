package com.georgevik.turnia.ui.main.eventtypes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.georgevik.turnia.navigation.EventTypeKind
import com.georgevik.turnia.navigation.LocalNavigator
import com.georgevik.turnia.navigation.MainRoute
import com.georgevik.turnia.ui.main.eventtypes.model.EventTypeMasterHeaderUi
import com.georgevik.turnia.ui.main.eventtypes.model.EventTypeMasterRowUi
import com.georgevik.turnia.ui.system.components.AcronymBadge
import com.georgevik.turnia.ui.system.components.TListItem
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.calendar_back
import turnia.app.shared.generated.resources.event_types_create_personal
import turnia.app.shared.generated.resources.event_types_empty
import turnia.app.shared.generated.resources.event_types_search_hint
import turnia.app.shared.generated.resources.event_types_section_personal
import turnia.app.shared.generated.resources.event_types_title

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventMasterScreen(viewModel: EventMasterViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.event_types_title)) },
                navigationIcon = {
                    IconButton(onClick = navigator::goBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(Res.string.calendar_back),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            navigator.goTo(
                                MainRoute.EventTypeDetailKey(EventTypeKind.PERSONAL, null, null)
                            )
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(Res.string.event_types_create_personal),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "search") {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text(stringResource(Res.string.event_types_search_hint)) },
                )
            }

            if (uiState.sections.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(Res.string.event_types_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }

            uiState.sections.forEach { section ->
                item(key = "header-${section.kind}") {
                    SectionHeader(section = section)
                }
                items(section.rows, key = { it.typeId }) { row ->
                    EventTypeRow(
                        row = row,
                        onClick = {
                            navigator.goTo(
                                MainRoute.EventTypeDetailKey(row.kind, row.groupId, row.typeId)
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(section: EventTypeMasterHeaderUi) {
    Row(
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (section.kind == EventTypeKind.GROUP) {
            Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        val title = when (section.kind) {
            EventTypeKind.PERSONAL -> stringResource(Res.string.event_types_section_personal)
            EventTypeKind.GROUP -> section.name
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EventTypeRow(row: EventTypeMasterRowUi, onClick: () -> Unit) {
    TListItem(
        title = if (row.acronym != null) "${row.acronym} · ${row.name}" else row.name,
        onClick = onClick,
        leading = { AcronymBadge(color = row.color, acronym = row.acronym) },
    )
}
