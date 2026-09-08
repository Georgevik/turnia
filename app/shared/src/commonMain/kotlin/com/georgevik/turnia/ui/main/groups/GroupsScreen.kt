package com.georgevik.turnia.ui.main.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.georgevik.turnia.navigation.LocalNavigator
import com.georgevik.turnia.navigation.main.routes.ExternalCalendarData
import com.georgevik.turnia.navigation.main.routes.MainRoute
import com.georgevik.turnia.ui.main.groups.components.GroupCard
import com.georgevik.turnia.ui.main.groups.components.GroupsFabMenu
import com.georgevik.turnia.ui.main.groups.components.GroupsFilterChips
import com.georgevik.turnia.ui.main.groups.components.JoinGroupSheet
import com.georgevik.turnia.ui.main.groups.components.JoinRequestCard
import com.georgevik.turnia.ui.main.groups.model.GroupRowUi
import com.georgevik.turnia.ui.main.groups.model.GroupsFilter
import com.georgevik.turnia.ui.main.groups.model.JoinRequestRowUi
import com.georgevik.turnia.ui.main.system.EmptyState
import com.georgevik.turnia.ui.main.system.ScreenHeader
import com.georgevik.turnia.ui.system.LocalSnackbar
import com.georgevik.turnia.ui.system.TurniaSnackbarVisual
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.group_detail_create
import turnia.app.shared.generated.resources.groups_empty_body
import turnia.app.shared.generated.resources.groups_empty_title
import turnia.app.shared.generated.resources.groups_filter_mine_empty_body
import turnia.app.shared.generated.resources.groups_filter_mine_empty_title
import turnia.app.shared.generated.resources.groups_join_already_member
import turnia.app.shared.generated.resources.groups_join_code_not_found
import turnia.app.shared.generated.resources.groups_join_error
import turnia.app.shared.generated.resources.groups_join_invitation_expired
import turnia.app.shared.generated.resources.groups_join_invitation_inactive
import turnia.app.shared.generated.resources.groups_join_joined
import turnia.app.shared.generated.resources.groups_join_requested
import turnia.app.shared.generated.resources.groups_load_error
import turnia.app.shared.generated.resources.groups_request_dismiss_error
import turnia.app.shared.generated.resources.groups_title

/**
 * "Grupos" tab: the groups this user belongs to. Tapping one opens its calendar, and the group
 * itself — where an admin edits it — hangs off the info button in that calendar's title bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(viewModel: GroupsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    val snackbar = LocalSnackbar.current
    val success = state as? GroupsUi.Success
    var joinSheetOpen by rememberSaveable { mutableStateOf(false) }
    val joinSheetState = rememberModalBottomSheetState()

    success?.userMessage?.let { message ->
        val visual = TurniaSnackbarVisual(message.text(), isError = message.isError)
        LaunchedEffect(message) {
            // The sheet would cover the snackbar, and the answer to the code is the whole point of
            // the round trip. A rejected code survives in the field for the next attempt.
            if (message != GroupsMessage.LoadFailed) joinSheetOpen = false
            snackbar.showSnackbar(visual)
            viewModel.hideSnackbar()
        }
    }

    Scaffold(
        floatingActionButton = {
            if (success != null) {
                GroupsFabMenu(
                    onJoin = { joinSheetOpen = true },
                    onCreate = { navigator.goTo(MainRoute.GroupDetail(groupId = null)) },
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ScreenHeader(
                title = stringResource(Res.string.groups_title),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            )

            when (val current = state) {
                GroupsUi.Loading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

                is GroupsUi.Success -> {
                    // A request outstanding is not an empty screen: it is the one thing the user
                    // is waiting on, and the empty state would cover it.
                    if (current.groups.isEmpty() && current.requests.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.GroupAdd,
                            title = stringResource(Res.string.groups_empty_title),
                            body = stringResource(Res.string.groups_empty_body),
                            action = stringResource(Res.string.group_detail_create),
                            onAction = {
                                navigator.goTo(MainRoute.GroupDetail(groupId = null))
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        GroupsFilterChips(
                            selected = current.filter,
                            pendingCount = current.requests.size,
                            onSelected = viewModel::filterSelected,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )

                        val groups = current.groups
                            .takeIf { current.filter != GroupsFilter.PENDING }
                            .orEmpty()
                        val requests = current.requests
                            .takeIf { current.filter != GroupsFilter.MINE }
                            .orEmpty()

                        if (groups.isEmpty() && requests.isEmpty()) {
                            NoGroupsYet()
                        } else {
                            GroupList(groups = groups, requests = requests)
                        }
                    }
                }
            }
        }
    }

    if (joinSheetOpen && success != null) {
        ModalBottomSheet(
            onDismissRequest = { joinSheetOpen = false },
            sheetState = joinSheetState,
        ) {
            JoinGroupSheet(
                code = success.joinCode,
                inProgress = success.joinInProgress,
                onCodeChanged = viewModel::joinCodeChanged,
                onSubmit = viewModel::requestToJoin,
            )
        }
    }
}

@Composable
private fun GroupList(
    groups: List<GroupRowUi>,
    requests: List<JoinRequestRowUi>,
) {
    val navigator = LocalNavigator.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(groups, key = { it.id.value }) { group ->
            GroupCard(
                group = group,
                onClick = {
                    val route = MainRoute.ExternalCalendar(
                        ExternalCalendarData.Group(
                            id = group.id.value,
                            name = group.name
                        )
                    )
                    navigator.goTo(route)
                },
            )
        }

        items(requests, key = { "request-${it.groupId.value}" }) { request ->
            JoinRequestCard(request = request)
        }
    }
}

/** Only "Tus grupos" can come up empty: the pending chip is gone once there is nothing pending. */
@Composable
private fun NoGroupsYet() = EmptyState(
    icon = Icons.Default.GroupAdd,
    title = stringResource(Res.string.groups_filter_mine_empty_title),
    body = stringResource(Res.string.groups_filter_mine_empty_body),
    modifier = Modifier.fillMaxSize(),
)

@Composable
private fun GroupsMessage.text(): String = stringResource(
    when (this) {
        GroupsMessage.LoadFailed -> Res.string.groups_load_error
        GroupsMessage.Joined -> Res.string.groups_join_joined
        GroupsMessage.JoinRequested -> Res.string.groups_join_requested
        GroupsMessage.AlreadyMember -> Res.string.groups_join_already_member
        GroupsMessage.JoinCodeNotFound -> Res.string.groups_join_code_not_found
        GroupsMessage.JoinInvitationInactive -> Res.string.groups_join_invitation_inactive
        GroupsMessage.JoinInvitationExpired -> Res.string.groups_join_invitation_expired
        GroupsMessage.JoinFailed -> Res.string.groups_join_error
        GroupsMessage.RequestDismissFailed -> Res.string.groups_request_dismiss_error
    }
)

private val GroupsMessage.isError: Boolean
    get() = when (this) {
        GroupsMessage.Joined,
        GroupsMessage.JoinRequested,
        GroupsMessage.AlreadyMember -> false

        GroupsMessage.LoadFailed,
        GroupsMessage.JoinCodeNotFound,
        GroupsMessage.JoinInvitationInactive,
        GroupsMessage.JoinInvitationExpired,
        GroupsMessage.JoinFailed,
        GroupsMessage.RequestDismissFailed -> true
    }
