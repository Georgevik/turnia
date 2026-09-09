package com.geoviksoft.turnia.ui.components.daydetail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geoviksoft.turnia.ui.components.calendar.model.CalendarEventUi
import com.geoviksoft.turnia.ui.components.daydetail.components.DayDetailAddEvent
import com.geoviksoft.turnia.ui.components.daydetail.components.DayDetailHeader
import com.geoviksoft.turnia.ui.components.daydetail.components.DayEventRow
import com.geoviksoft.turnia.ui.components.daydetail.model.AddEventTypesError
import com.geoviksoft.turnia.ui.components.daydetail.model.AddEventTypesUi
import com.geoviksoft.turnia.ui.components.daydetail.model.DaySwapMessage
import com.geoviksoft.turnia.ui.system.LocalSnackbar
import com.geoviksoft.turnia.ui.system.components.TurniaErrorContent
import com.geoviksoft.turnia.ui.system.toErrorSnackbar
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.day_detail_load_error
import turnia.app.shared.generated.resources.event_details_empty
import turnia.app.shared.generated.resources.event_note_cancel
import turnia.app.shared.generated.resources.event_note_dialog_title
import turnia.app.shared.generated.resources.event_note_error
import turnia.app.shared.generated.resources.event_note_hint
import turnia.app.shared.generated.resources.event_note_save
import turnia.app.shared.generated.resources.event_remove_cancel
import turnia.app.shared.generated.resources.event_remove_confirm
import turnia.app.shared.generated.resources.event_remove_confirm_body
import turnia.app.shared.generated.resources.event_remove_confirm_title
import turnia.app.shared.generated.resources.event_swap_error_not_assignee
import turnia.app.shared.generated.resources.event_swap_error_not_swappable
import turnia.app.shared.generated.resources.event_swap_error_not_found
import turnia.app.shared.generated.resources.event_swap_error_not_member
import turnia.app.shared.generated.resources.event_swap_error_own_shift
import turnia.app.shared.generated.resources.event_swap_error_save
import turnia.app.shared.generated.resources.event_swap_error_taken_by_someone
import turnia.app.shared.generated.resources.event_swap_take_cancel
import turnia.app.shared.generated.resources.event_swap_take_confirm
import turnia.app.shared.generated.resources.event_swap_take_confirm_body
import turnia.app.shared.generated.resources.event_swap_take_confirm_title

@Composable
fun DayDetailSheet(
    date: LocalDate,
    events: List<CalendarEventUi>,
    addMode: DayAddMode,
    openEditTypeScreen: (groupId: String, groupName: String) -> Unit,
    openNewPersonalTypeScreen: () -> Unit,
    onClose: (shouldRefresh: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DayDetailSheetViewModel = koinViewModel(key = date.toString()) {
        parametersOf(date, addMode)
    },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var adding by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<CalendarEventUi?>(null) }
    var editingNotes by remember { mutableStateOf<CalendarEventUi?>(null) }
    var pendingTake by remember { mutableStateOf<CalendarEventUi?>(null) }

    val noteError by viewModel.noteError.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbar.current
    if (noteError) {
        val text = stringResource(Res.string.event_note_error)
        LaunchedEffect(Unit) {
            snackbar.showSnackbar(text.toErrorSnackbar())
            viewModel.noteErrorShown()
        }
    }

    val swapMessage by viewModel.swapMessage.collectAsStateWithLifecycle()
    swapMessage?.let { message ->
        val text = message.text()
        LaunchedEffect(message) {
            snackbar.showSnackbar(text.toErrorSnackbar())
            viewModel.swapMessageShown()
        }
    }

    editingNotes?.let { event ->
        NotesDialog(
            initialNotes = event.notes.orEmpty(),
            onDismiss = { editingNotes = null },
            onSave = { notes ->
                viewModel.saveNotes(event, notes)
                editingNotes = null
            },
        )
    }

    pendingTake?.let { event ->
        AlertDialog(
            onDismissRequest = { pendingTake = null },
            title = { Text(stringResource(Res.string.event_swap_take_confirm_title)) },
            text = { Text(stringResource(Res.string.event_swap_take_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.takeEvent(event)
                    pendingTake = null
                }) {
                    Text(stringResource(Res.string.event_swap_take_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingTake = null }) {
                    Text(stringResource(Res.string.event_swap_take_cancel))
                }
            },
        )
    }

    pendingDelete?.let { event ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(Res.string.event_remove_confirm_title)) },
            text = { Text(stringResource(Res.string.event_remove_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeEvent(event)
                    pendingDelete = null
                    onClose(true)
                }) { Text(stringResource(Res.string.event_remove_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(Res.string.event_remove_cancel))
                }
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 560.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 16.dp),
    ) {
        DayDetailHeader(
            date = date,
            eventCount = events.size,
            adding = adding,
            showAdd = addMode.canAdd,
            onToggleAdd = { adding = !adding },
        )

        Spacer(Modifier.height(16.dp))

        Box {
                AnimatedContent(adding, transitionSpec = {
                    fadeIn() togetherWith fadeOut(animationSpec = tween(90))
                }) { isAdding ->
                    if (isAdding) {
                        // Only the add pane needs the loaded types; the day's events arrive as a
                        // parameter, so they must stay on screen while these load or fail.
                        when (val state = uiState) {
                            AddEventTypesUi.Loading -> AddPaneLoading()

                            is AddEventTypesUi.Error -> TurniaErrorContent(
                                message = state.error.message(),
                                modifier = Modifier.fillMaxWidth(),
                                onRetry = viewModel::retry,
                            )

                            is AddEventTypesUi.Success -> DayDetailAddEvent(
                                addMode = addMode,
                                sections = state.sections,
                                onPickEventType = { eventType ->
                                    viewModel.addEventOfType(eventType)
                                    onClose(true)
                                },
                                onEditGroup = openEditTypeScreen,
                                onAddPersonalEventType = openNewPersonalTypeScreen,
                            )
                        }
                    } else if (events.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.event_details_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    } else {
                        Column {
                            events.forEachIndexed { index, event ->
                                if (index > 0) Spacer(Modifier.height(12.dp))
                                DayEventRow(
                                    event = event,
                                    onRemove = if (event.removable) {
                                        { pendingDelete = event }
                                    } else {
                                        null
                                    },
                                    onEditNotes = if (event.notesEditable) {
                                        { editingNotes = event }
                                    } else {
                                        null
                                    },
                                    onSwapChange = if (event.canOfferSwap) {
                                        { onSwap -> viewModel.setOnSwap(event, onSwap) }
                                    } else {
                                        null
                                    },
                                    onTake = if (event.canTake) {
                                        { pendingTake = event }
                                    } else {
                                        null
                                    },
                                )
                            }
                        }
                    }
                }
        }
    }
}

@Composable
private fun NotesDialog(
    initialNotes: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by remember(initialNotes) { mutableStateOf(initialNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.event_note_dialog_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(stringResource(Res.string.event_note_hint)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) {
                Text(stringResource(Res.string.event_note_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.event_note_cancel))
            }
        },
    )
}

@Composable
private fun AddPaneLoading() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun AddEventTypesError.message(): String = stringResource(
    when (this) {
        AddEventTypesError.LoadFailed -> Res.string.day_detail_load_error
    }
)

@Composable
private fun DaySwapMessage.text(): String = stringResource(
    when (this) {
        DaySwapMessage.NotAssignee -> Res.string.event_swap_error_not_assignee
        DaySwapMessage.NotSwappable -> Res.string.event_swap_error_not_swappable
        DaySwapMessage.NotMember -> Res.string.event_swap_error_not_member
        DaySwapMessage.OwnShift -> Res.string.event_swap_error_own_shift
        DaySwapMessage.NotFound -> Res.string.event_swap_error_not_found
        DaySwapMessage.TakenBySomeoneElse -> Res.string.event_swap_error_taken_by_someone
        DaySwapMessage.SaveFailed -> Res.string.event_swap_error_save
    }
)
