package com.geoviksoft.turnia.ui.main.settings.mygroups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geoviksoft.turnia.navigation.LocalNavigator
import com.geoviksoft.turnia.navigation.main.routes.ExternalCalendarData
import com.geoviksoft.turnia.navigation.main.routes.MainRoute
import com.geoviksoft.turnia.ui.main.groups.components.GroupCard
import com.geoviksoft.turnia.ui.main.system.EmptyState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.calendar_back
import turnia.app.shared.generated.resources.groups_empty_body
import turnia.app.shared.generated.resources.groups_empty_title
import turnia.app.shared.generated.resources.settings_my_groups

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyGroupsScreen(viewModel: MyGroupsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings_my_groups)) },
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
        when (val current = state) {
            MyGroupsUi.Loading -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            is MyGroupsUi.Success -> if (current.groups.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.GroupAdd,
                    title = stringResource(Res.string.groups_empty_title),
                    body = stringResource(Res.string.groups_empty_body),
                    modifier = Modifier.padding(innerPadding),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(current.groups, key = { it.id.value }) { group ->
                        GroupCard(
                            group = group,
                            onClick = {
                                // A revoked user cannot read the group document, so there is no
                                // info to open: what is left of the group is its calendar.
                                val route = if (group.isRevoked) {
                                    MainRoute.ExternalCalendar(
                                        ExternalCalendarData.Group(id = group.id.value, name = group.name),
                                    )
                                } else {
                                    MainRoute.GroupDetail(group.id.value)
                                }
                                navigator.goTo(route)
                            },
                        )
                    }
                }
            }
        }
    }
}
