package com.georgevik.turnia.ui.group.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.georgevik.turnia.navigation.LocalNavigator
import com.georgevik.turnia.navigation.root.routes.RootRoute
import com.georgevik.turnia.navigation.routes.EventTypeDetailData
import com.georgevik.turnia.ui.group.detail.model.GroupDetailMessage
import com.georgevik.turnia.ui.group.detail.model.GroupDetailScreenError
import com.georgevik.turnia.ui.group.detail.model.GroupDetailUi
import com.georgevik.turnia.ui.group.detail.model.GroupTypeRowUi
import com.georgevik.turnia.ui.system.LocalSnackbar
import com.georgevik.turnia.ui.system.components.AcronymBadge
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
import turnia.app.shared.generated.resources.group_detail_error_load
import turnia.app.shared.generated.resources.group_detail_error_not_found
import turnia.app.shared.generated.resources.group_detail_error_save
import turnia.app.shared.generated.resources.group_detail_field_invitation
import turnia.app.shared.generated.resources.group_detail_field_members
import turnia.app.shared.generated.resources.group_detail_field_name
import turnia.app.shared.generated.resources.group_detail_readonly
import turnia.app.shared.generated.resources.group_detail_save
import turnia.app.shared.generated.resources.group_detail_section_types
import turnia.app.shared.generated.resources.group_detail_title_new
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
                    onSave = viewModel::onSave,
                    onTypeClick = { row ->
                        navigator.goTo(
                            RootRoute.EventTypeDetailKey(
                                EventTypeDetailData.EditGroup(
                                    typeId = row.typeId,
                                    groupId = row.groupId,
                                )
                            )
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun GroupDetailContent(
    modifier: Modifier = Modifier,
    state: GroupDetailUi.Success,
    onNameChanged: (String) -> Unit,
    onSave: () -> Unit,
    onTypeClick: (GroupTypeRowUi) -> Unit,
) {
    val form = state.form

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Avatar(
                background = entityColor(form.groupId.ifBlank { form.name }),
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

        if (!state.isNew) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TReadOnlyField(
                    label = stringResource(Res.string.group_detail_field_members),
                    value = pluralStringResource(
                        Res.plurals.group_member_count,
                        form.memberCount,
                        form.memberCount,
                    ),
                    modifier = Modifier.weight(1f),
                )
                TReadOnlyField(
                    label = stringResource(Res.string.group_detail_field_invitation),
                    value = form.invitationCode,
                    modifier = Modifier.weight(1f),
                    valueFontFamily = FontFamily.Monospace,
                )
            }
        }

        if (!form.editable) {
            Caption(stringResource(Res.string.group_detail_readonly))
        }

        EventTypesSection(state = state, onTypeClick = onTypeClick)

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

@Composable
private fun EventTypesSection(
    state: GroupDetailUi.Success,
    onTypeClick: (GroupTypeRowUi) -> Unit,
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
    }
)
