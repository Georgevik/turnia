package com.georgevik.turnia.ui.main.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.georgevik.turnia.navigation.LocalNavigator
import com.georgevik.turnia.navigation.LocalRootNavigator
import com.georgevik.turnia.navigation.main.routes.ExternalCalendarData
import com.georgevik.turnia.navigation.main.routes.MainRoute
import com.georgevik.turnia.navigation.root.routes.RootRoute
import com.georgevik.turnia.ui.main.groups.components.GroupCard
import com.georgevik.turnia.ui.main.system.EmptyState
import com.georgevik.turnia.ui.system.LocalSnackbar
import com.georgevik.turnia.ui.system.toErrorSnackbar
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.group_detail_create
import turnia.app.shared.generated.resources.groups_empty_body
import turnia.app.shared.generated.resources.groups_empty_title
import turnia.app.shared.generated.resources.groups_load_error
import turnia.app.shared.generated.resources.groups_search_hint

/**
 * "Grupos" tab: the groups this user belongs to. Tapping one opens its calendar; the info button
 * opens the group itself, which is where an admin edits it.
 */
@Composable
fun GroupsScreen(viewModel: GroupsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    // Group detail covers the whole screen, so it goes on the root stack, not this tab's.
    val rootNavigator = LocalRootNavigator.current
    val snackbar = LocalSnackbar.current
    val success = state as? GroupsUi.Success

    success?.userMessage?.let { message ->
        val text = stringResource(Res.string.groups_load_error)
        LaunchedEffect(message) {
            snackbar.showSnackbar(text.toErrorSnackbar())
            viewModel.userMessageShown()
        }
    }

    Scaffold(
        floatingActionButton = {
            if (success?.groups?.isNotEmpty() == true) {
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(Res.string.group_detail_create)) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    onClick = { rootNavigator.goTo(RootRoute.GroupDetailKey(groupId = "")) },
                )
            }
        },
    ) { innerPadding ->
        val content = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))

        when (val current = state) {
            GroupsUi.Loading -> Box(content, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            is GroupsUi.Success -> if (current.groups.isEmpty() && current.query.isBlank()) {
                // The empty state offers the action itself, so the button would be a second one.
                EmptyState(
                    icon = Icons.Default.GroupAdd,
                    title = stringResource(Res.string.groups_empty_title),
                    body = stringResource(Res.string.groups_empty_body),
                    action = stringResource(Res.string.group_detail_create),
                    onAction = { rootNavigator.goTo(RootRoute.GroupDetailKey(groupId = "")) },
                    modifier = content,
                )
            } else {
                LazyColumn(
                    modifier = content.padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "search") {
                        OutlinedTextField(
                            value = current.query,
                            onValueChange = viewModel::searchBy,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            placeholder = { Text(stringResource(Res.string.groups_search_hint)) },
                        )
                    }

                    items(current.groups, key = { it.id.value }) { group ->
                        GroupCard(
                            group = group,
                            onClick = {
                                navigator.goTo(
                                    MainRoute.ExternalCalendar(
                                        ExternalCalendarData.Group(group.id.value, group.name)
                                    )
                                )
                            },
                            onDetails = {
                                rootNavigator.goTo(RootRoute.GroupDetailKey(group.id.value))
                            },
                        )
                    }
                }
            }
        }
    }
}
