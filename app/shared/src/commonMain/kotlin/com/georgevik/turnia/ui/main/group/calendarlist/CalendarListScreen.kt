package com.georgevik.turnia.ui.main.group.calendarlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.georgevik.turnia.navigation.LocalNavigator
import com.georgevik.turnia.navigation.LocalRootNavigator
import com.georgevik.turnia.navigation.main.routes.ExternalCalendarData
import com.georgevik.turnia.navigation.main.routes.MainRoute
import com.georgevik.turnia.navigation.root.routes.RootRoute
import com.georgevik.turnia.ui.main.group.externalcalendar.components.ColleagueCard
import com.georgevik.turnia.ui.main.group.externalcalendar.components.GroupCard
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.group_detail_create
import turnia.app.shared.generated.resources.group_groups_header
import turnia.app.shared.generated.resources.group_people_header
import turnia.app.shared.generated.resources.group_search_hint
import turnia.app.shared.generated.resources.group_see_all

@Composable
fun CalendarListScreen(viewModel: CalendarListViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    // Group detail covers the whole screen, so it goes on the root stack, not this tab's.
    val rootNavigator = LocalRootNavigator.current
    var query by rememberSaveable { mutableStateOf("") }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "search") {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.searchBy(it)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text(stringResource(Res.string.group_search_hint)) },
            )
        }

        if (uiState.colleagues.isNotEmpty()) {
            item(key = "colleagues-header") {
                SectionHeader(
                    title = stringResource(Res.string.group_people_header),
                    action = stringResource(Res.string.group_see_all),
                    onAction = null,
                )
            }
            items(uiState.colleagues, key = { it.id.value }) { colleague ->
                ColleagueCard(
                    colleague = colleague,
                    onClick = {
                        val route = MainRoute.ExternalCalendar(
                            ExternalCalendarData.Personal(colleague.id.value, colleague.name)
                        )
                        navigator.goTo(route)
                    },
                )
            }
        }

        if (uiState.groups.isNotEmpty()) {
            item(key = "groups-header") {
                SectionHeader(
                    title = stringResource(Res.string.group_groups_header),
                    action = stringResource(Res.string.group_detail_create),
                    onAction = { rootNavigator.goTo(RootRoute.GroupDetailKey(groupId = "")) },
                )
            }
            items(uiState.groups, key = { it.id.value }) { group ->
                GroupCard(
                    group = group,
                    onClick = {
                        val route = MainRoute.ExternalCalendar(
                            ExternalCalendarData.Group(group.id.value, group.name)
                        )
                        navigator.goTo(route)
                    },
                    onDetails = { rootNavigator.goTo(RootRoute.GroupDetailKey(group.id.value)) },
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(action, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
