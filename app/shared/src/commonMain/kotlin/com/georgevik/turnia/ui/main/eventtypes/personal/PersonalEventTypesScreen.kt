package com.georgevik.turnia.ui.main.eventtypes.personal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.georgevik.turnia.navigation.LocalNavigator
import com.georgevik.turnia.navigation.root.routes.RootRoute
import com.georgevik.turnia.navigation.routes.EventTypeDetailData
import com.georgevik.turnia.ui.main.eventtypes.personal.model.PersonalEventTypeRowUi
import com.georgevik.turnia.ui.main.eventtypes.personal.model.PersonalEventTypesMessage
import com.georgevik.turnia.ui.main.eventtypes.personal.model.PersonalEventTypesUi
import com.georgevik.turnia.ui.system.LocalSnackbar
import com.georgevik.turnia.ui.system.components.AcronymBadge
import com.georgevik.turnia.ui.system.components.ConfirmationDialog
import com.georgevik.turnia.ui.system.components.TListItem
import com.georgevik.turnia.ui.system.toErrorSnackbar
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.calendar_back
import turnia.app.shared.generated.resources.dialog_cancel
import turnia.app.shared.generated.resources.event_type_field_end
import turnia.app.shared.generated.resources.event_type_field_start
import turnia.app.shared.generated.resources.personal_event_types_add
import turnia.app.shared.generated.resources.personal_event_types_delete
import turnia.app.shared.generated.resources.personal_event_types_delete_dialog_message
import turnia.app.shared.generated.resources.personal_event_types_delete_dialog_title
import turnia.app.shared.generated.resources.personal_event_types_empty
import turnia.app.shared.generated.resources.personal_event_types_error_delete
import turnia.app.shared.generated.resources.personal_event_types_title

/**
 * "Mis eventos": the user's own event types, where they create, edit and delete them. Tapping one
 * opens the event type detail; both live on the root back stack, over Main.
 */
@Composable
fun PersonalEventTypesScreen(viewModel: PersonalEventTypesViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    val snackbar = LocalSnackbar.current
    var typeToDelete by remember { mutableStateOf<PersonalEventTypeRowUi?>(null) }

    // The detail edits and creates types on its own; re-read them when coming back from it.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshEvents()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.personal_event_types_title)) },
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
            ExtendedFloatingActionButton(
                onClick = {
                    navigator.goTo(
                        RootRoute.EventTypeDetailKey(EventTypeDetailData.NewPersonal)
                    )
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(Res.string.personal_event_types_add)) },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier.fillMaxSize().padding(innerPadding)

        when (val state = uiState) {
            PersonalEventTypesUi.Loading -> Box(contentModifier, Alignment.Center) {
                CircularProgressIndicator()
            }

            is PersonalEventTypesUi.Success -> {
                state.userMessage?.let { message ->
                    val text = message.message()
                    LaunchedEffect(message) {
                        snackbar.showSnackbar(text.toErrorSnackbar())
                        viewModel.userMessageShown()
                    }
                }

                if (state.types.isEmpty()) {
                    Box(contentModifier.padding(24.dp), Alignment.Center) {
                        Text(
                            text = stringResource(Res.string.personal_event_types_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = contentModifier,
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 12.dp,
                            bottom = 96.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.types, key = { it.typeId.value }) { row ->
                            PersonalEventTypeRow(
                                row = row,
                                onClick = {
                                    navigator.goTo(
                                        RootRoute.EventTypeDetailKey(
                                            EventTypeDetailData.EditPersonal(row.typeId.value)
                                        )
                                    )
                                },
                                onDelete = { typeToDelete = row },
                            )
                        }
                    }
                }
            }
        }
    }

    typeToDelete?.let { row ->
        ConfirmationDialog(
            title = stringResource(Res.string.personal_event_types_delete_dialog_title),
            message = stringResource(
                Res.string.personal_event_types_delete_dialog_message,
                row.name,
            ),
            confirmText = stringResource(Res.string.personal_event_types_delete),
            dismissText = stringResource(Res.string.dialog_cancel),
            onConfirm = {
                typeToDelete = null
                viewModel.onDelete(row.typeId)
            },
            onDismissRequest = { typeToDelete = null },
        )
    }
}

@Composable
private fun PersonalEventTypeRow(
    row: PersonalEventTypeRowUi,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    TListItem(
        title = row.name,
        subtitle = row.schedule(),
        onClick = onClick,
        leading = { AcronymBadge(color = row.color, acronym = row.acronym) },
        trailing = {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = stringResource(Res.string.personal_event_types_delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}

@Composable
private fun PersonalEventTypeRowUi.schedule(): String? = when {
    startTime != null && endTime != null -> "$startTime – $endTime"
    startTime != null -> "${stringResource(Res.string.event_type_field_start)} $startTime"
    endTime != null -> "${stringResource(Res.string.event_type_field_end)} $endTime"
    else -> null
}

@Composable
private fun PersonalEventTypesMessage.message(): String = stringResource(
    when (this) {
        PersonalEventTypesMessage.DeleteFailed -> Res.string.personal_event_types_error_delete
    }
)
