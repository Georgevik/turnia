package com.georgevik.turnia.ui.main.settings.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.georgevik.turnia.navigation.LocalNavigator
import com.georgevik.turnia.navigation.root.routes.RootRoute
import com.georgevik.turnia.ui.main.group.calendarlist.model.GroupRowUi
import com.georgevik.turnia.ui.main.group.externalcalendar.components.GroupCard
import com.georgevik.turnia.ui.system.LocalSnackbar
import com.georgevik.turnia.ui.system.toErrorSnackbar
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.calendar_back
import turnia.app.shared.generated.resources.group_detail_create
import turnia.app.shared.generated.resources.settings_groups_empty
import turnia.app.shared.generated.resources.settings_groups_load_error
import turnia.app.shared.generated.resources.settings_groups_title

/**
 * The groups this user belongs to. Both the row and its info button open the same detail screen —
 * there is nothing else to do with a group from here.
 */
@Composable
fun GroupsScreen(viewModel: GroupsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    val snackbar = LocalSnackbar.current
    val success = state as? GroupsUi.Success

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    success?.userMessage?.let { message ->
        val text = stringResource(Res.string.settings_groups_load_error)
        LaunchedEffect(message) {
            snackbar.showSnackbar(text.toErrorSnackbar())
            viewModel.userMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings_groups_title)) },
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navigator.goTo(RootRoute.GroupDetailKey(groupId = "")) }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.group_detail_create),
                )
            }
        },
    ) { innerPadding ->
        val content = Modifier.fillMaxSize().padding(innerPadding)

        when (val current = state) {
            GroupsUi.Loading -> Box(content, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            is GroupsUi.Success -> GroupList(
                modifier = content,
                groups = current.groups,
                onOpen = { group -> navigator.goTo(RootRoute.GroupDetailKey(group.id.value)) },
            )
        }
    }
}

@Composable
private fun GroupList(
    modifier: Modifier,
    groups: List<GroupRowUi>,
    onOpen: (GroupRowUi) -> Unit,
) {
    if (groups.isEmpty()) {
        Box(modifier.padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(Res.string.settings_groups_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(groups, key = { it.id.value }) { group ->
            GroupCard(
                group = group,
                onClick = { onOpen(group) },
                onDetails = { onOpen(group) },
            )
        }
    }
}
