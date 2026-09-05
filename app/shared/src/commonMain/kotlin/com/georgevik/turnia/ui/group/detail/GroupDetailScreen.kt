package com.georgevik.turnia.ui.group.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.navigation.LocalNavigator
import com.georgevik.turnia.navigation.root.routes.RootRoute
import com.georgevik.turnia.navigation.routes.EventTypeDetailData
import com.georgevik.turnia.ui.group.detail.model.GroupDetailMessage
import com.georgevik.turnia.ui.group.detail.model.GroupDetailScreenError
import com.georgevik.turnia.ui.group.detail.model.GroupDetailUi
import com.georgevik.turnia.ui.group.detail.model.GroupMemberUi
import com.georgevik.turnia.ui.group.detail.model.GroupTypeRowUi
import com.georgevik.turnia.ui.group.detail.model.JoinRequestUi
import com.georgevik.turnia.ui.system.LocalSnackbar
import com.georgevik.turnia.ui.system.components.ConfirmationDialog
import com.georgevik.turnia.ui.system.components.AcronymBadge
import com.georgevik.turnia.ui.system.components.AdminBadge
import com.georgevik.turnia.ui.system.components.Avatar
import com.georgevik.turnia.ui.system.components.Chevron
import com.georgevik.turnia.ui.system.components.TFieldLabel
import com.georgevik.turnia.ui.system.components.TListItem
import com.georgevik.turnia.ui.system.components.TReadOnlyField
import com.georgevik.turnia.ui.system.components.TurniaDialogError
import com.georgevik.turnia.ui.system.components.TurniaErrorContent
import com.georgevik.turnia.ui.system.entityColor
import com.georgevik.turnia.ui.system.toErrorSnackbar
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.calendar_back
import turnia.app.shared.generated.resources.event_type_field_end
import turnia.app.shared.generated.resources.event_type_field_start
import turnia.app.shared.generated.resources.group_detail_auto_approve
import turnia.app.shared.generated.resources.group_detail_auto_approve_off
import turnia.app.shared.generated.resources.group_detail_auto_approve_on
import turnia.app.shared.generated.resources.group_detail_code_changed
import turnia.app.shared.generated.resources.group_detail_code_hidden
import turnia.app.shared.generated.resources.group_detail_code_regenerate
import turnia.app.shared.generated.resources.group_detail_error_load
import turnia.app.shared.generated.resources.group_detail_error_not_found
import turnia.app.shared.generated.resources.dialog_cancel
import turnia.app.shared.generated.resources.group_detail_error_remove_member
import turnia.app.shared.generated.resources.group_detail_error_request
import turnia.app.shared.generated.resources.group_detail_member_remove
import turnia.app.shared.generated.resources.group_detail_member_remove_confirm
import turnia.app.shared.generated.resources.group_detail_member_remove_message
import turnia.app.shared.generated.resources.group_detail_member_remove_title
import turnia.app.shared.generated.resources.group_detail_error_save
import turnia.app.shared.generated.resources.group_detail_field_invitation
import turnia.app.shared.generated.resources.group_detail_field_name
import turnia.app.shared.generated.resources.group_detail_members_can_see_code
import turnia.app.shared.generated.resources.group_detail_members_sheet_title
import turnia.app.shared.generated.resources.group_detail_readonly
import turnia.app.shared.generated.resources.group_detail_request_accept
import turnia.app.shared.generated.resources.group_detail_request_reject
import turnia.app.shared.generated.resources.group_detail_requests_title
import turnia.app.shared.generated.resources.group_detail_save
import turnia.app.shared.generated.resources.group_detail_section_invitation
import turnia.app.shared.generated.resources.group_detail_section_members
import turnia.app.shared.generated.resources.group_detail_section_types
import turnia.app.shared.generated.resources.group_detail_title_new
import turnia.app.shared.generated.resources.group_detail_types_add
import turnia.app.shared.generated.resources.group_detail_types_empty
import turnia.app.shared.generated.resources.group_detail_types_new_hint
import turnia.app.shared.generated.resources.group_member_count

/**
 * Group detail: view, edit or create a group, with its event types listed at the bottom. It is a
 * root-level destination, so it covers Main's bottom bar and pushes the event type detail onto the
 * root back stack.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(viewModel: GroupDetailViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    val snackbar = LocalSnackbar.current

    var membersSheetOpen by remember { mutableStateOf(false) }
    var pendingRemoval by remember { mutableStateOf<GroupMemberUi?>(null) }
    val sheetState = rememberModalBottomSheetState()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val state = uiState
                    if (state is GroupDetailUi.Success) {
                        Text(
                            text = state.form.name.ifBlank {
                                stringResource(Res.string.group_detail_title_new)
                            },
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                        )
                    }
                },
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
        val contentModifier = Modifier.fillMaxSize().padding(innerPadding)

        when (val state = uiState) {
            GroupDetailUi.Loading -> Box(contentModifier, Alignment.Center) {
                CircularProgressIndicator()
            }

            is GroupDetailUi.Error -> when (state.error) {
                GroupDetailScreenError.NotFound -> TurniaDialogError(
                    message = stringResource(Res.string.group_detail_error_not_found),
                    onDismiss = navigator::goBack,
                )

                GroupDetailScreenError.LoadFailed -> TurniaErrorContent(
                    message = stringResource(Res.string.group_detail_error_load),
                    modifier = contentModifier,
                    onRetry = viewModel::retry,
                )
            }

            is GroupDetailUi.Success -> {
                state.userMessage?.let { message ->
                    val text = message.message()
                    LaunchedEffect(message) {
                        snackbar.showSnackbar(text.toErrorSnackbar())
                        viewModel.userMessageShown()
                    }
                }
                LaunchedEffect(state.isSaved) {
                    if (state.isSaved) navigator.goBack()
                }

                GroupDetailContent(
                    modifier = contentModifier.padding(20.dp),
                    state = state,
                    onNameChanged = viewModel::onNameChanged,
                    onAutoApproveChanged = viewModel::onAutoApproveChanged,
                    onMembersCanSeeCodeChanged = viewModel::onMembersCanSeeCodeChanged,
                    onRegenerateCode = viewModel::onRegenerateCode,
                    onAcceptRequest = viewModel::onAcceptRequest,
                    onRejectRequest = viewModel::onRejectRequest,
                    onMembersClick = { membersSheetOpen = true },
                    onSave = viewModel::onSave,
                    onTypeClick = { row ->
                        navigator.goTo(
                            RootRoute.EventTypeDetailKey(
                                EventTypeDetailData.EditGroup(
                                    typeId = row.typeId.value,
                                    groupId = row.groupId.value,
                                )
                            )
                        )
                    },
                    onAddType = {
                        navigator.goTo(
                            RootRoute.EventTypeDetailKey(
                                EventTypeDetailData.NewGroup(groupId = state.form.groupId.value)
                            )
                        )
                    },
                )
            }
        }
    }

    val success = uiState as? GroupDetailUi.Success
    if (membersSheetOpen && success != null) {
        ModalBottomSheet(
            onDismissRequest = { membersSheetOpen = false },
            sheetState = sheetState,
        ) {
            MembersSheet(
                members = success.members,
                // Admins only, and never another admin: removing one is a demotion, which does
                // not exist. That also covers the admin looking at their own row — they leave
                // through the group's calendar instead.
                canRemove = success.form.editable,
                onRemove = { pendingRemoval = it },
            )
        }
    }

    pendingRemoval?.let { member ->
        ConfirmationDialog(
            title = stringResource(
                Res.string.group_detail_member_remove_title,
                member.name.ifBlank { member.username },
            ),
            message = stringResource(Res.string.group_detail_member_remove_message),
            confirmText = stringResource(Res.string.group_detail_member_remove_confirm),
            dismissText = stringResource(Res.string.dialog_cancel),
            onConfirm = {
                pendingRemoval = null
                viewModel.onRemoveMember(member.id)
            },
            onDismissRequest = { pendingRemoval = null },
        )
    }
}

@Composable
private fun GroupDetailContent(
    modifier: Modifier = Modifier,
    state: GroupDetailUi.Success,
    onNameChanged: (String) -> Unit,
    onAutoApproveChanged: (Boolean) -> Unit,
    onMembersCanSeeCodeChanged: (Boolean) -> Unit,
    onRegenerateCode: () -> Unit,
    onAcceptRequest: (UserId) -> Unit,
    onRejectRequest: (UserId) -> Unit,
    onMembersClick: () -> Unit,
    onSave: () -> Unit,
    onTypeClick: (GroupTypeRowUi) -> Unit,
    onAddType: () -> Unit,
) {
    val form = state.form

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Avatar(
                background = entityColor(form.groupId.value.ifBlank { form.name }),
                icon = Icons.Default.Groups,
                size = 72.dp,
            )
        }

        if (form.editable) {
            OutlinedTextField(
                value = form.name,
                onValueChange = onNameChanged,
                label = { Text(stringResource(Res.string.group_detail_field_name)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            TReadOnlyField(stringResource(Res.string.group_detail_field_name), form.name)
        }

        // Somebody is waiting at the door: it goes first, before anything an admin might browse.
        if (state.joinRequests.isNotEmpty()) {
            JoinRequestsSection(
                requests = state.joinRequests,
                onAccept = onAcceptRequest,
                onReject = onRejectRequest,
            )
        }

        if (!state.isNew) {
            InvitationSection(
                form = form,
                onAutoApproveChanged = onAutoApproveChanged,
                onMembersCanSeeCodeChanged = onMembersCanSeeCodeChanged,
                onRegenerateCode = onRegenerateCode,
            )

            MembersSection(memberCount = form.memberCount, onClick = onMembersClick)
        }

        if (!form.editable) {
            Caption(stringResource(Res.string.group_detail_readonly))
        }

        EventTypesSection(
            state = state,
            onTypeClick = onTypeClick,
            onAddType = onAddType,
        )

        if (form.editable) {
            Button(
                onClick = onSave,
                enabled = form.name.isNotBlank() && !state.saving,
                shape = RoundedCornerShape(percent = 50),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (state.saving) {
                    CircularProgressIndicator()
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(Res.string.group_detail_save),
                        modifier = Modifier.padding(start = 8.dp),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/** Pending requests, in the colour the rest of the screen does not use: they need an answer. */
@Composable
private fun JoinRequestsSection(
    requests: List<JoinRequestUi>,
    onAccept: (UserId) -> Unit,
    onReject: (UserId) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(Res.string.group_detail_requests_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            requests.forEach { request ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = request.name.ifBlank { request.username },
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (request.username.isNotBlank()) {
                            Text(
                                text = "@${request.username}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    IconButton(onClick = { onAccept(request.userId) }) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(
                                Res.string.group_detail_request_accept
                            ),
                        )
                    }
                    IconButton(onClick = { onReject(request.userId) }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(
                                Res.string.group_detail_request_reject
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InvitationSection(
    form: GroupDetailUi.GroupForm,
    onAutoApproveChanged: (Boolean) -> Unit,
    onMembersCanSeeCodeChanged: (Boolean) -> Unit,
    onRegenerateCode: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TFieldLabel(stringResource(Res.string.group_detail_section_invitation))

        ToggleRow(
            title = stringResource(Res.string.group_detail_auto_approve),
            subtitle = if (form.autoApprove) {
                stringResource(Res.string.group_detail_auto_approve_on)
            } else {
                stringResource(Res.string.group_detail_auto_approve_off)
            },
            checked = form.autoApprove,
            enabled = form.editable,
            onCheckedChange = onAutoApproveChanged,
        )

        if (form.invitationCode != null) {
            InvitationCode(
                code = form.invitationCode,
                // Rotating the code is how an admin closes a door they left open by hand; with
                // auto-approve on there is no door to close, the code is the group's front page.
                onRegenerate = onRegenerateCode.takeIf { form.editable && !form.autoApprove },
            )
            if (form.codeChanged) {
                Caption(stringResource(Res.string.group_detail_code_changed))
            }
        } else {
            Caption(stringResource(Res.string.group_detail_code_hidden))
        }

        if (form.editable) {
            ToggleRow(
                title = stringResource(Res.string.group_detail_members_can_see_code),
                subtitle = null,
                checked = form.membersCanSeeCode,
                enabled = true,
                onCheckedChange = onMembersCanSeeCodeChanged,
            )
        }
    }
}

@Composable
private fun InvitationCode(code: String, onRegenerate: (() -> Unit)?) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.group_detail_field_invitation),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = code,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (onRegenerate != null) {
                IconButton(onClick = onRegenerate) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(
                            Res.string.group_detail_code_regenerate
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun MembersSection(memberCount: Int, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TFieldLabel(stringResource(Res.string.group_detail_section_members))

        TListItem(
            title = pluralStringResource(Res.plurals.group_member_count, memberCount, memberCount),
            onClick = onClick,
            leading = {
                Avatar(
                    background = MaterialTheme.colorScheme.secondaryContainer,
                    icon = Icons.Default.Groups,
                    iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            },
            trailing = { Chevron() },
        )
    }
}

@Composable
private fun MembersSheet(
    members: List<GroupMemberUi>,
    canRemove: Boolean,
    onRemove: (GroupMemberUi) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "title") {
            Text(
                text = stringResource(Res.string.group_detail_members_sheet_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        items(members, key = { it.id.value }) { member ->
            TListItem(
                title = member.name.ifBlank { member.username },
                subtitle = "@${member.username}".takeIf { member.username.isNotBlank() },
                leading = {
                    Avatar(
                        background = entityColor(member.id.value),
                        icon = Icons.Default.Person,
                    )
                },
                trailing = {
                    if (member.isAdmin) {
                        AdminBadge()
                    } else if (canRemove) {
                        IconButton(onClick = { onRemove(member) }) {
                            Icon(
                                imageVector = Icons.Default.PersonRemove,
                                contentDescription = stringResource(
                                    Res.string.group_detail_member_remove
                                ),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun EventTypesSection(
    state: GroupDetailUi.Success,
    onTypeClick: (GroupTypeRowUi) -> Unit,
    onAddType: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TFieldLabel(stringResource(Res.string.group_detail_section_types))

        when {
            // A group has to exist before an admin can hang event types off it.
            state.isNew -> Caption(stringResource(Res.string.group_detail_types_new_hint))

            state.eventTypes.isEmpty() -> Caption(
                stringResource(Res.string.group_detail_types_empty)
            )

            else -> state.eventTypes.forEach { row ->
                EventTypeRow(row = row, onClick = { onTypeClick(row) })
            }
        }

        if (state.form.editable && !state.isNew) {
            OutlinedButton(
                onClick = onAddType,
                shape = RoundedCornerShape(percent = 50),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(Res.string.group_detail_types_add),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun EventTypeRow(row: GroupTypeRowUi, onClick: () -> Unit) {
    TListItem(
        title = row.name,
        subtitle = row.schedule(),
        onClick = onClick,
        leading = { AcronymBadge(color = row.color, acronym = row.acronym) },
        trailing = { Chevron() },
    )
}

/** "08:00 · Inicio" style summary, or `null` when the type carries no times. */
@Composable
private fun GroupTypeRowUi.schedule(): String? = when {
    startTime != null && endTime != null -> "$startTime – $endTime"
    startTime != null -> "${stringResource(Res.string.event_type_field_start)} $startTime"
    endTime != null -> "${stringResource(Res.string.event_type_field_end)} $endTime"
    else -> null
}

@Composable
private fun Caption(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GroupDetailMessage.message(): String = stringResource(
    when (this) {
        GroupDetailMessage.SaveFailed -> Res.string.group_detail_error_save
        GroupDetailMessage.RequestFailed -> Res.string.group_detail_error_request
        GroupDetailMessage.RemoveMemberFailed -> Res.string.group_detail_error_remove_member
    }
)
