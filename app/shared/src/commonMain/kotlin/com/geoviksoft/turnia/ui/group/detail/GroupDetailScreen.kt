package com.geoviksoft.turnia.ui.group.detail

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geoviksoft.turnia.core.domain.model.InvitationLink
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.navigation.LocalNavigator
import com.geoviksoft.turnia.navigation.LocalRootNavigator
import com.geoviksoft.turnia.navigation.root.routes.RootRoute
import com.geoviksoft.turnia.navigation.routes.EventTypeDetailData
import com.geoviksoft.turnia.ui.group.detail.model.GroupCloseUi
import com.geoviksoft.turnia.ui.group.detail.model.GroupDetailMessage
import com.geoviksoft.turnia.ui.group.detail.model.GroupDetailScreenError
import com.geoviksoft.turnia.ui.group.detail.model.GroupDetailUi
import com.geoviksoft.turnia.ui.group.detail.model.GroupMemberUi
import com.geoviksoft.turnia.ui.group.detail.model.GroupTypeRowUi
import com.geoviksoft.turnia.ui.group.detail.model.JoinRequestUi
import com.geoviksoft.turnia.ui.system.LocalSnackbar
import com.geoviksoft.turnia.ui.system.TurniaSnackbarVisual
import com.geoviksoft.turnia.ui.system.color.EntityPalette
import com.geoviksoft.turnia.ui.system.components.AcronymBadge
import com.geoviksoft.turnia.ui.system.components.AdminBadge
import com.geoviksoft.turnia.ui.system.components.Avatar
import com.geoviksoft.turnia.ui.system.components.Chevron
import com.geoviksoft.turnia.ui.system.components.ColorSwatchPicker
import com.geoviksoft.turnia.ui.system.components.ConfirmationDialog
import com.geoviksoft.turnia.ui.system.components.ConfirmationStatus
import com.geoviksoft.turnia.ui.system.components.TFieldLabel
import com.geoviksoft.turnia.ui.system.components.TListItem
import com.geoviksoft.turnia.ui.system.components.TReadOnlyField
import com.geoviksoft.turnia.ui.system.components.TurniaDialogError
import com.geoviksoft.turnia.ui.system.components.TurniaErrorContent
import com.geoviksoft.turnia.ui.system.components.UserAvatar
import com.geoviksoft.turnia.ui.system.components.UserAvatarSize
import com.geoviksoft.turnia.ui.system.keyboardAware
import com.geoviksoft.turnia.ui.system.rememberTextSharer
import com.geoviksoft.turnia.ui.system.toErrorSnackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.calendar_back
import turnia.app.shared.generated.resources.dialog_cancel
import turnia.app.shared.generated.resources.event_type_field_end
import turnia.app.shared.generated.resources.event_type_field_start
import turnia.app.shared.generated.resources.group_delete_action
import turnia.app.shared.generated.resources.group_delete_confirm
import turnia.app.shared.generated.resources.group_delete_error
import turnia.app.shared.generated.resources.group_delete_message
import turnia.app.shared.generated.resources.group_delete_not_empty_error
import turnia.app.shared.generated.resources.group_delete_title
import turnia.app.shared.generated.resources.group_detail_auto_approve
import turnia.app.shared.generated.resources.group_detail_auto_approve_off
import turnia.app.shared.generated.resources.group_detail_auto_approve_on
import turnia.app.shared.generated.resources.group_detail_code_changed
import turnia.app.shared.generated.resources.group_detail_code_copied
import turnia.app.shared.generated.resources.group_detail_code_copy
import turnia.app.shared.generated.resources.group_detail_code_hidden
import turnia.app.shared.generated.resources.group_detail_code_regenerate
import turnia.app.shared.generated.resources.group_detail_code_share
import turnia.app.shared.generated.resources.group_detail_code_share_text
import turnia.app.shared.generated.resources.group_detail_create
import turnia.app.shared.generated.resources.group_detail_error_load
import turnia.app.shared.generated.resources.group_detail_error_not_found
import turnia.app.shared.generated.resources.group_detail_error_remove_member
import turnia.app.shared.generated.resources.group_detail_error_request
import turnia.app.shared.generated.resources.group_detail_error_save
import turnia.app.shared.generated.resources.group_detail_field_invitation
import turnia.app.shared.generated.resources.group_detail_field_name
import turnia.app.shared.generated.resources.group_detail_member_remove
import turnia.app.shared.generated.resources.group_detail_member_remove_confirm
import turnia.app.shared.generated.resources.group_detail_member_remove_message
import turnia.app.shared.generated.resources.group_detail_member_remove_title
import turnia.app.shared.generated.resources.group_detail_members_can_see_code
import turnia.app.shared.generated.resources.group_detail_members_sheet_title
import turnia.app.shared.generated.resources.group_detail_readonly
import turnia.app.shared.generated.resources.group_detail_request_accept
import turnia.app.shared.generated.resources.group_detail_request_reject
import turnia.app.shared.generated.resources.group_detail_requests_title
import turnia.app.shared.generated.resources.group_detail_save
import turnia.app.shared.generated.resources.group_detail_section_color
import turnia.app.shared.generated.resources.group_detail_section_invitation
import turnia.app.shared.generated.resources.group_detail_section_members
import turnia.app.shared.generated.resources.group_detail_section_types
import turnia.app.shared.generated.resources.group_detail_title_new
import turnia.app.shared.generated.resources.group_detail_types_add
import turnia.app.shared.generated.resources.group_detail_types_empty
import turnia.app.shared.generated.resources.group_detail_types_empty_body
import turnia.app.shared.generated.resources.group_leave_action
import turnia.app.shared.generated.resources.group_leave_confirm
import turnia.app.shared.generated.resources.group_leave_error
import turnia.app.shared.generated.resources.group_leave_last_admin_error
import turnia.app.shared.generated.resources.group_leave_message
import turnia.app.shared.generated.resources.group_leave_title
import turnia.app.shared.generated.resources.group_member_count

/**
 * Group detail: view, edit or create a group, with its event types listed at the bottom. It sits on
 * Main's tab stacks, so leaving or deleting the group can clear every one of them; the event type
 * detail still covers the bottom bar and goes on the root stack.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(viewModel: GroupDetailViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    val rootNavigator = LocalRootNavigator.current
    val snackbar = LocalSnackbar.current

    var membersSheetOpen by remember { mutableStateOf(false) }
    var memberActions by remember { mutableStateOf<GroupMemberUi?>(null) }
    var pendingRemoval by remember { mutableStateOf<GroupMemberUi?>(null) }
    var leaveRequested by remember { mutableStateOf(false) }
    var deleteRequested by remember { mutableStateOf(false) }
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
                actions = {
                    val state = uiState
                    if (state is GroupDetailUi.Success && !state.isNew) {
                        GroupExitAction(
                            isAdmin = state.form.editable,
                            enabled = !state.saving && state.close == null,
                            onLeave = { leaveRequested = true },
                            onDelete = { deleteRequested = true },
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
                LaunchedEffect(state.isSaved, state.hasLeft) {
                    when {
                        state.hasLeft -> navigator.popToRoot()
                        state.isSaved -> navigator.goBack()
                    }
                }

                GroupDetailContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .keyboardAware(innerPadding)
                        .padding(20.dp),
                    state = state,
                    onNameChanged = viewModel::onNameChanged,
                    onPickColor = viewModel::onPickColor,
                    onAutoApproveChanged = viewModel::onAutoApproveChanged,
                    onMembersCanSeeCodeChanged = viewModel::onMembersCanSeeCodeChanged,
                    onRegenerateCode = viewModel::onRegenerateCode,
                    onAcceptRequest = viewModel::onAcceptRequest,
                    onRejectRequest = viewModel::onRejectRequest,
                    onMembersClick = { membersSheetOpen = true },
                    onSave = viewModel::onSave,
                    onTypeClick = { row ->
                        rootNavigator.goTo(
                            RootRoute.EventTypeDetailKey(
                                EventTypeDetailData.EditGroup(
                                    typeId = row.typeId.value,
                                    groupId = row.groupId?.value,
                                )
                            )
                        )
                    },
                    onAddType = {
                        rootNavigator.goTo(
                            RootRoute.EventTypeDetailKey(
                                EventTypeDetailData.NewGroup(
                                    groupId = state.form.groupId?.value
                                )
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
                canManage = success.form.editable,
                onMemberLongPress = { memberActions = it },
            )
        }
    }

    memberActions?.let { member ->
        ModalBottomSheet(onDismissRequest = { memberActions = null }) {
            MemberActionsSheet(
                member = member,
                onRemove = {
                    memberActions = null
                    pendingRemoval = member
                },
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

    val close = success?.close
    LaunchedEffect(close) {
        if (close is GroupCloseUi.Succeeded || close is GroupCloseUi.Failed) {
            delay(CLOSE_RESULT_MILLIS)
            leaveRequested = false
            deleteRequested = false
            viewModel.closeResultShown()
        }
    }

    if (leaveRequested) {
        ConfirmationDialog(
            title = stringResource(Res.string.group_leave_title),
            message = stringResource(Res.string.group_leave_message),
            confirmText = stringResource(Res.string.group_leave_confirm),
            dismissText = stringResource(Res.string.dialog_cancel),
            status = close.toStatus(),
            onConfirm = viewModel::onLeaveGroup,
            onDismissRequest = { leaveRequested = false },
        )
    }

    if (deleteRequested && success != null) {
        val canDelete = success.form.memberCount <= 1
        ConfirmationDialog(
            title = stringResource(Res.string.group_delete_title),
            message = stringResource(
                if (canDelete) Res.string.group_delete_message
                else Res.string.group_delete_not_empty_error
            ),
            confirmText = stringResource(Res.string.group_delete_confirm),
            dismissText = stringResource(Res.string.dialog_cancel),
            confirmEnabled = canDelete,
            status = close.toStatus(),
            onConfirm = viewModel::onDeleteGroup,
            onDismissRequest = { deleteRequested = false },
        )
    }
}

/** How long the tick or the cross stays up before the outcome is acted on. */
private const val CLOSE_RESULT_MILLIS = 1_000L

private fun GroupCloseUi?.toStatus() = when (this) {
    GroupCloseUi.Running -> ConfirmationStatus.Running
    GroupCloseUi.Succeeded -> ConfirmationStatus.Succeeded
    is GroupCloseUi.Failed -> ConfirmationStatus.Failed
    null -> ConfirmationStatus.Idle
}

@Composable
private fun GroupExitAction(
    isAdmin: Boolean,
    enabled: Boolean,
    onLeave: () -> Unit,
    onDelete: () -> Unit,
) {
    IconButton(onClick = if (isAdmin) onDelete else onLeave, enabled = enabled) {
        Icon(
            imageVector = if (isAdmin) {
                Icons.Default.DeleteOutline
            } else {
                Icons.AutoMirrored.Filled.Logout
            },
            contentDescription = stringResource(
                if (isAdmin) Res.string.group_delete_action else Res.string.group_leave_action
            ),
            tint = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun GroupDetailContent(
    modifier: Modifier = Modifier,
    state: GroupDetailUi.Success,
    onNameChanged: (String) -> Unit,
    onPickColor: (Color) -> Unit,
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
                background = form.color,
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

        // The colour is the group's, so it goes with the rest of what only an admin may change.
        if (form.editable) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TFieldLabel(stringResource(Res.string.group_detail_section_color))
                ColorSwatchPicker(
                    colors = EntityPalette,
                    selected = form.color,
                    onPick = onPickColor,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Somebody is waiting at the door: it goes first, before anything an admin might browse.
        if (state.joinRequests.isNotEmpty()) {
            JoinRequestsSection(
                requests = state.joinRequests,
                onAccept = onAcceptRequest,
                onReject = onRejectRequest,
            )
        }

        InvitationSection(
            form = form,
            onAutoApproveChanged = onAutoApproveChanged,
            onMembersCanSeeCodeChanged = onMembersCanSeeCodeChanged,
            onRegenerateCode = onRegenerateCode,
        )

        if (!state.isNew) {
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
                // A group is created with at least one event type, or it can hold no shift at all.
                enabled = form.name.isNotBlank() &&
                        !state.saving &&
                        (!state.isNew || state.eventTypes.isNotEmpty()),
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
                        text = stringResource(
                            if (state.isNew) Res.string.group_detail_create
                            else Res.string.group_detail_save
                        ),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    UserAvatar(
                        avatar = request.avatar,
                        size = UserAvatarSize.M
                    )
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

        if (form.editable) {
            ToggleRow(
                title = stringResource(Res.string.group_detail_auto_approve),
                subtitle = if (form.autoApprove) {
                    stringResource(Res.string.group_detail_auto_approve_on)
                } else {
                    stringResource(Res.string.group_detail_auto_approve_off)
                },
                checked = form.autoApprove,
                enabled = true,
                onCheckedChange = onAutoApproveChanged,
            )
        }

        val invitationCode = form.invitationCode
        if (!invitationCode.isNullOrBlank()) {
            val sharer = rememberTextSharer()
            val snackbar = LocalSnackbar.current
            val scope = rememberCoroutineScope()
            val copied = stringResource(Res.string.group_detail_code_copied)
            val shareText = stringResource(
                Res.string.group_detail_code_share_text,
                form.name,
                InvitationLink.of(invitationCode),
            )

            InvitationCode(
                code = invitationCode,
                onCopy = if (form.canPassOnCode) {
                    {
                        sharer.copy(invitationCode)
                        // iOS copies without a word, so the app is the only one that can say so.
                        scope.launch { snackbar.showSnackbar(TurniaSnackbarVisual(copied)) }
                    }
                } else {
                    null
                },
                onShare = if (form.canPassOnCode) {
                    { sharer.share(shareText) }
                } else {
                    null
                },
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
private fun InvitationCode(
    code: String,
    onCopy: (() -> Unit)?,
    onShare: (() -> Unit)?,
    onRegenerate: (() -> Unit)?,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        enabled = onCopy != null,
                        onClickLabel = stringResource(Res.string.group_detail_code_copy),
                        onClick = { onCopy?.invoke() },
                    )
                    .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
            ) {
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
            if (onShare != null) {
                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = stringResource(Res.string.group_detail_code_share),
                    )
                }
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
    canManage: Boolean,
    onMemberLongPress: (GroupMemberUi) -> Unit,
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
            val manageable = canManage && !member.isAdmin

            TListItem(
                title = member.name.ifBlank { member.username },
                subtitle = "@${member.username}".takeIf { member.username.isNotBlank() },
                onLongClick = { onMemberLongPress(member) }.takeIf { manageable },
                leading = { UserAvatar(avatar = member.avatar) },
                trailing = { if (member.isAdmin) AdminBadge() },
            )
        }
    }
}

@Composable
private fun MemberActionsSheet(
    member: GroupMemberUi,
    onRemove: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = member.name.ifBlank { member.username },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        TListItem(
            title = stringResource(Res.string.group_detail_member_remove),
            onClick = onRemove,
            leading = {
                Icon(
                    imageVector = Icons.Default.PersonRemove,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
        )
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

        // Until it has one, the group cannot hold a single shift — worth more than a caption.
        if (state.eventTypes.isEmpty()) {
            TypesEmptyCallout()
        } else {
            state.eventTypes.forEach { row ->
                EventTypeRow(row = row, onClick = { onTypeClick(row) })
            }
        }

        if (state.form.editable) {
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

/** A group with no event types cannot hold a shift, so the gap is stated, not whispered. */
@Composable
private fun TypesEmptyCallout() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(Res.string.group_detail_types_empty),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(Res.string.group_detail_types_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
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
        GroupDetailMessage.LeaveFailed -> Res.string.group_leave_error
        GroupDetailMessage.LeaveLastAdmin -> Res.string.group_leave_last_admin_error
        GroupDetailMessage.DeleteFailed -> Res.string.group_delete_error
        GroupDetailMessage.DeleteNotEmpty -> Res.string.group_delete_not_empty_error
    }
)
