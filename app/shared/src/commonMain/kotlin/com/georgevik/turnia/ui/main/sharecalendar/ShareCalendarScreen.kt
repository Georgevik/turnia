package com.georgevik.turnia.ui.main.sharecalendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.navigation.LocalNavigator
import com.georgevik.turnia.ui.system.LocalSnackbar
import com.georgevik.turnia.ui.system.components.Avatar
import com.georgevik.turnia.ui.system.components.ConfirmationDialog
import com.georgevik.turnia.ui.system.components.TListItem
import com.georgevik.turnia.ui.system.entityColor
import com.georgevik.turnia.ui.system.toErrorSnackbar
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.calendar_back
import turnia.app.shared.generated.resources.dialog_cancel
import turnia.app.shared.generated.resources.share_calendar_add
import turnia.app.shared.generated.resources.share_calendar_already_shared
import turnia.app.shared.generated.resources.share_calendar_empty
import turnia.app.shared.generated.resources.share_calendar_grant_error
import turnia.app.shared.generated.resources.share_calendar_load_error
import turnia.app.shared.generated.resources.share_calendar_revoke
import turnia.app.shared.generated.resources.share_calendar_revoke_confirm
import turnia.app.shared.generated.resources.share_calendar_revoke_error
import turnia.app.shared.generated.resources.share_calendar_revoke_message
import turnia.app.shared.generated.resources.share_calendar_revoke_title
import turnia.app.shared.generated.resources.share_calendar_search_empty
import turnia.app.shared.generated.resources.share_calendar_search_hint
import turnia.app.shared.generated.resources.share_calendar_search_min
import turnia.app.shared.generated.resources.share_calendar_title
import turnia.app.shared.generated.resources.share_calendar_unknown_user

/**
 * The people who can see this user's calendar, and the two ways to change that list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareCalendarScreen(viewModel: ShareCalendarViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    val snackbar = LocalSnackbar.current

    var sheetOpen by remember { mutableStateOf(false) }
    var pendingRevoke by remember { mutableStateOf<SharedUserUi?>(null) }
    val sheetState = rememberModalBottomSheetState()

    val success = state as? ShareCalendarUi.Success

    success?.userMessage?.let { message ->
        val text = message.message()
        LaunchedEffect(message) {
            snackbar.showSnackbar(text.toErrorSnackbar())
            viewModel.userMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.share_calendar_title)) },
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
            // Nothing to share with until the list this compares against has loaded.
            if (success != null) {
                FloatingActionButton(onClick = { sheetOpen = true }) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = stringResource(Res.string.share_calendar_add),
                    )
                }
            }
        },
    ) { innerPadding ->
        val content = Modifier.fillMaxSize().padding(innerPadding)

        when (val current = state) {
            ShareCalendarUi.Loading -> Box(content, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            is ShareCalendarUi.Success -> SharedList(
                modifier = content,
                sharedWith = current.sharedWith,
                onRevoke = { user -> pendingRevoke = user },
            )
        }
    }

    if (sheetOpen && success != null) {
        ModalBottomSheet(
            onDismissRequest = {
                sheetOpen = false
                viewModel.onSearchDismissed()
            },
            sheetState = sheetState,
        ) {
            SearchContent(
                search = success.search,
                onQueryChanged = viewModel::onSearchChanged,
                onPick = { userId ->
                    sheetOpen = false
                    viewModel.onGrant(userId)
                },
            )
        }
    }

    pendingRevoke?.let { user ->
        ConfirmationDialog(
            title = stringResource(Res.string.share_calendar_revoke_title),
            message = stringResource(Res.string.share_calendar_revoke_message, user.displayName()),
            confirmText = stringResource(Res.string.share_calendar_revoke_confirm),
            dismissText = stringResource(Res.string.dialog_cancel),
            onConfirm = {
                pendingRevoke = null
                viewModel.onRevoke(user.id)
            },
            onDismissRequest = { pendingRevoke = null },
        )
    }
}

@Composable
private fun SharedList(
    modifier: Modifier,
    sharedWith: List<SharedUserUi>,
    onRevoke: (SharedUserUi) -> Unit,
) {
    if (sharedWith.isEmpty()) {
        Box(modifier.padding(32.dp), contentAlignment = Alignment.Center) {
            Message(stringResource(Res.string.share_calendar_empty))
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(sharedWith, key = { it.id.value }) { user ->
            TListItem(
                title = user.displayName(),
                subtitle = user.username.takeIf { it.isNotBlank() }?.let { "@$it" },
                leading = { UserAvatar(user.id) },
                trailing = {
                    IconButton(onClick = { onRevoke(user) }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(Res.string.share_calendar_revoke),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun SearchContent(
    search: SearchUi,
    onQueryChanged: (String) -> Unit,
    onPick: (UserId) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = search.query,
            onValueChange = onQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            prefix = { Text("@") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text(stringResource(Res.string.share_calendar_search_hint)) },
        )

        when (val panel = search.panel) {
            SearchUi.Panel.Searching -> Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            SearchUi.Panel.TooShort ->
                Message(stringResource(Res.string.share_calendar_search_min))

            SearchUi.Panel.Empty ->
                Message(stringResource(Res.string.share_calendar_search_empty))

            is SearchUi.Panel.Results -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(panel.users, key = { it.id.value }) { result ->
                    TListItem(
                        title = result.name.ifBlank {
                            stringResource(Res.string.share_calendar_unknown_user)
                        },
                        subtitle = "@${result.username}",
                        onClick = if (result.alreadyShared) null else ({ onPick(result.id) }),
                        leading = { UserAvatar(result.id) },
                        trailing = if (result.alreadyShared) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(Res.string.share_calendar_already_shared),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun UserAvatar(id: UserId) =
    Avatar(background = entityColor(id.value), icon = Icons.Default.Person)

@Composable
private fun Message(text: String) = Text(
    text = text,
    style = MaterialTheme.typography.bodyLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    textAlign = TextAlign.Center,
    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
)

@Composable
private fun SharedUserUi.displayName(): String =
    name.ifBlank { stringResource(Res.string.share_calendar_unknown_user) }

@Composable
private fun ShareCalendarMessage.message(): String = stringResource(
    when (this) {
        ShareCalendarMessage.LoadFailed -> Res.string.share_calendar_load_error
        ShareCalendarMessage.GrantFailed -> Res.string.share_calendar_grant_error
        ShareCalendarMessage.RevokeFailed -> Res.string.share_calendar_revoke_error
    }
)
