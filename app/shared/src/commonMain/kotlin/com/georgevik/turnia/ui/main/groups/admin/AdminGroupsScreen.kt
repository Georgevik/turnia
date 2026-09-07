package com.georgevik.turnia.ui.main.groups.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.georgevik.turnia.navigation.LocalNavigator
import com.georgevik.turnia.navigation.main.routes.MainRoute
import com.georgevik.turnia.ui.main.groups.components.GroupCard
import com.georgevik.turnia.ui.main.system.EmptyState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.admin_groups_caption
import turnia.app.shared.generated.resources.admin_groups_empty_body
import turnia.app.shared.generated.resources.admin_groups_empty_title
import turnia.app.shared.generated.resources.admin_groups_title
import turnia.app.shared.generated.resources.calendar_back
import turnia.app.shared.generated.resources.group_detail_create

/**
 * "Grupos que administras", reached from Ajustes: the groups this user can actually edit. The
 * Grupos tab lists every group they belong to; this one is the way into editing the few they run.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminGroupsScreen(viewModel: AdminGroupsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.admin_groups_title)) },
                navigationIcon = {
                    IconButton(onClick = navigator::goBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(Res.string.calendar_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val content = Modifier.fillMaxSize().padding(innerPadding)
        val fill = Modifier.fillMaxSize()

        when (val current = state) {
            AdminGroupsUi.Loading -> Box(content, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            is AdminGroupsUi.Success -> if (current.groups.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Groups,
                    title = stringResource(Res.string.admin_groups_empty_title),
                    body = stringResource(Res.string.admin_groups_empty_body),
                    action = stringResource(Res.string.group_detail_create),
                    onAction = { navigator.goTo(MainRoute.GroupDetail(groupId = "")) },
                    modifier = content,
                )
            } else {
                Column(modifier = content) {
                    Text(
                        text = stringResource(Res.string.admin_groups_caption),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )

                    LazyColumn(
                        modifier = fill,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(current.groups, key = { it.id.value }) { group ->
                            GroupCard(
                                group = group,
                                // Every row here is one they administer: the badge would be noise.
                                showAdminBadge = false,
                                onClick = {
                                    navigator.goTo(
                                        MainRoute.GroupDetail(groupId = group.id.value)
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
