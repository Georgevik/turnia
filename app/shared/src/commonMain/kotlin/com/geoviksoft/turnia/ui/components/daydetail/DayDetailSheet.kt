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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geoviksoft.turnia.core.domain.model.EventId
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.ui.components.calendar.model.DayEventUi
import com.geoviksoft.turnia.ui.components.calendar.model.EventSource
import com.geoviksoft.turnia.ui.components.calendar.model.HOURS_SEPARATOR
import com.geoviksoft.turnia.ui.components.calendar.model.TransferHolderUi
import com.geoviksoft.turnia.ui.components.daydetail.components.DayDetailAddEvent
import com.geoviksoft.turnia.ui.components.daydetail.components.DayDetailHeader
import com.geoviksoft.turnia.ui.components.daydetail.components.DayEventRow
import com.geoviksoft.turnia.ui.components.daydetail.components.PreviewEventTypeSections
import com.geoviksoft.turnia.ui.components.daydetail.model.AddEventTypesError
import com.geoviksoft.turnia.ui.components.daydetail.model.AddEventTypesUi
import com.geoviksoft.turnia.ui.components.daydetail.model.DaySwapMessage
import com.geoviksoft.turnia.ui.components.daydetail.model.EventTypeUi
import com.geoviksoft.turnia.ui.system.LocalSnackbar
import com.geoviksoft.turnia.ui.system.PreviewTurniaTheme
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
import turnia.app.shared.generated.resources.event_return_confirm
import turnia.app.shared.generated.resources.event_return_confirm_body
import turnia.app.shared.generated.resources.event_return_confirm_title
import turnia.app.shared.generated.resources.event_swap_error_not_assignee
import turnia.app.shared.generated.resources.event_swap_error_not_found
import turnia.app.shared.generated.resources.event_swap_error_not_member
import turnia.app.shared.generated.resources.event_swap_error_not_swappable
import turnia.app.shared.generated.resources.event_swap_error_nothing_to_return
import turnia.app.shared.generated.resources.event_swap_error_own_shift
import turnia.app.shared.generated.resources.event_swap_error_previous_holder_left
import turnia.app.shared.generated.resources.event_swap_error_save
import turnia.app.shared.generated.resources.event_swap_error_taken_by_someone
import turnia.app.shared.generated.resources.event_swap_take_cancel
import turnia.app.shared.generated.resources.event_swap_take_confirm
import turnia.app.shared.generated.resources.event_swap_take_confirm_body
import turnia.app.shared.generated.resources.event_swap_take_confirm_title

@Composable
fun DayDetailSheet(
    date: LocalDate,
    events: List<DayEventUi>,
    addMode: DayAddMode,
    openEditTypeScreen: (groupId: String, groupName: String) -> Unit,
    openNewPersonalTypeScreen: () -> Unit,
    openNewGroupTypeScreen: (groupId: String) -> Unit,
    onClose: (shouldRefresh: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DayDetailSheetViewModel = koinViewModel(key = date.toString()) {
        parametersOf(date, addMode)
    },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var adding by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<DayEventUi?>(null) }
    var pendingReturn by remember { mutableStateOf<DayEventUi?>(null) }
    var editingNotes by remember { mutableStateOf<DayEventUi?>(null) }
    var pendingTake by remember { mutableStateOf<DayEventUi?>(null) }

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

    pendingReturn?.let { event ->
        AlertDialog(
            onDismissRequest = { pendingReturn = null },
            title = { Text(stringResource(Res.string.event_return_confirm_title)) },
            text = {
                Text(stringResource(Res.string.event_return_confirm_body, event.returnsTo.orEmpty()))
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.returnEvent(event)
                    pendingReturn = null
                }) { Text(stringResource(Res.string.event_return_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingReturn = null }) {
                    Text(stringResource(Res.string.event_remove_cancel))
                }
            },
        )
    }

    DayDetailContent(
        date = date,
        events = events,
        addMode = addMode,
        addTypes = uiState,
        adding = adding,
        onToggleAdd = { adding = !adding },
        onRetryTypes = viewModel::retry,
        onPickEventType = { eventType ->
            viewModel.addEventOfType(eventType)
            onClose(true)
        },
        onEditGroup = openEditTypeScreen,
        onAddPersonalEventType = openNewPersonalTypeScreen,
        onAddGroupEventType = openNewGroupTypeScreen,
        onRemove = { pendingDelete = it },
        onReturn = { pendingReturn = it },
        onEditNotes = { editingNotes = it },
        onSwapChange = viewModel::setOnSwap,
        onTake = { pendingTake = it },
        modifier = modifier,
    )
}

/** The sheet without its view model, so it can be previewed; the dialogs stay with the caller. */
@Composable
private fun DayDetailContent(
    date: LocalDate,
    events: List<DayEventUi>,
    addMode: DayAddMode,
    addTypes: AddEventTypesUi,
    adding: Boolean,
    onToggleAdd: () -> Unit,
    onRetryTypes: () -> Unit,
    onPickEventType: (EventTypeUi) -> Unit,
    onEditGroup: (groupId: String, groupName: String) -> Unit,
    onAddPersonalEventType: () -> Unit,
    onAddGroupEventType: (groupId: String) -> Unit,
    onRemove: (DayEventUi) -> Unit,
    onReturn: (DayEventUi) -> Unit,
    onEditNotes: (DayEventUi) -> Unit,
    onSwapChange: (DayEventUi, Boolean) -> Unit,
    onTake: (DayEventUi) -> Unit,
    modifier: Modifier = Modifier,
) {
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
            onToggleAdd = onToggleAdd,
        )

        Spacer(Modifier.height(16.dp))

        Box {
                AnimatedContent(adding, transitionSpec = {
                    fadeIn() togetherWith fadeOut(animationSpec = tween(90))
                }) { isAdding ->
                    if (isAdding) {
                        // Only the add pane needs the loaded types; the day's events arrive as a
                        // parameter, so they must stay on screen while these load or fail.
                        when (addTypes) {
                            AddEventTypesUi.Loading -> AddPaneLoading()

                            is AddEventTypesUi.Error -> TurniaErrorContent(
                                message = addTypes.error.message(),
                                modifier = Modifier.fillMaxWidth(),
                                onRetry = onRetryTypes,
                            )

                            is AddEventTypesUi.Success -> DayDetailAddEvent(
                                addMode = addMode,
                                sections = addTypes.sections,
                                onPickEventType = onPickEventType,
                                onEditGroup = onEditGroup,
                                onAddPersonalEventType = onAddPersonalEventType,
                                onAddGroupEventType = onAddGroupEventType,
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
                                    onRemove = when {
                                        event.removable -> {
                                            { onRemove(event) }
                                        }
                                        event.canReturn -> {
                                            { onReturn(event) }
                                        }
                                        else -> null
                                    },
                                    onEditNotes = if (event.notesEditable) {
                                        { onEditNotes(event) }
                                    } else {
                                        null
                                    },
                                    onSwapChange = if (event.canOfferSwap) {
                                        { onSwap -> onSwapChange(event, onSwap) }
                                    } else {
                                        null
                                    },
                                    onTake = if (event.canTake) {
                                        { onTake(event) }
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
        DaySwapMessage.NothingToReturn -> Res.string.event_swap_error_nothing_to_return
        DaySwapMessage.PreviousHolderLeft -> Res.string.event_swap_error_previous_holder_left
        DaySwapMessage.SaveFailed -> Res.string.event_swap_error_save
    }
)

// Previews. The labels are developer-facing, so they stay here rather than in composeResources.

private val previewDate = LocalDate(2026, 9, 10)

private val previewEvents = listOf(
    DayEventUi(
        id = EventId("preview-morning"),
        groupId = GroupId("preview-emergency"),
        ownerId = UserId("me"),
        assigneeId = UserId("me"),
        source = EventSource.GROUP,
        name = "Morning",
        acronym = "M",
        background = Color(0xFF039BE5),
        date = previewDate,
        timeRange = "08:00${HOURS_SEPARATOR}15:00",
        swappable = true,
        activeMember = true,
        isOwner = true,
        assigneeName = "Lucía Fernández",
        assigneeIsMe = true,
        groupName = "Emergency",
        removable = true,
    ),
    DayEventUi(
        id = EventId("preview-night"),
        groupId = GroupId("preview-emergency"),
        ownerId = UserId("marta"),
        assigneeId = UserId("carlos"),
        source = EventSource.GROUP,
        name = "Night",
        acronym = "N",
        background = Color(0xFF5E35B1),
        date = previewDate,
        timeRange = "22:00${HOURS_SEPARATOR}08:00",
        onSwap = true,
        swappable = true,
        activeMember = true,
        assigneeName = "Carlos Ruiz",
        groupName = "Emergency",
        transferChain = listOf(
            TransferHolderUi("Marta Gil", isMe = false),
            TransferHolderUi("Lucía Fernández", isMe = true),
            TransferHolderUi("Carlos Ruiz", isMe = false),
        ),
    ),
    DayEventUi(
        id = EventId("preview-course"),
        groupId = null,
        ownerId = null,
        assigneeId = null,
        source = EventSource.PERSONAL,
        name = "Training",
        acronym = "T",
        background = Color(0xFF00897B),
        date = previewDate,
        removable = true,
        notes = "Advanced life support course · Room 3, 4 pm",
        notesEditable = true,
    ),
)

@Composable
private fun PreviewDayDetail(
    events: List<DayEventUi>,
    adding: Boolean = false,
    addTypes: AddEventTypesUi = AddEventTypesUi.Loading,
) {
    PreviewTurniaTheme {
        Surface {
            DayDetailContent(
                date = previewDate,
                events = events,
                addMode = DayAddMode.Full,
                addTypes = addTypes,
                adding = adding,
                onToggleAdd = {},
                onRetryTypes = {},
                onPickEventType = {},
                onEditGroup = { _, _ -> },
                onAddPersonalEventType = {},
                onAddGroupEventType = {},
                onRemove = {},
                onReturn = {},
                onEditNotes = {},
                onSwapChange = { _, _ -> },
                onTake = {},
            )
        }
    }
}

/** A shift of mine, one on swap with a chain behind it, and a personal event with notes. */
@Preview
@Composable
fun DayDetailSheetPreview() {
    PreviewDayDetail(events = previewEvents)
}

@Preview
@Composable
fun DayDetailSheetEmptyPreview() {
    PreviewDayDetail(events = emptyList())
}

@Preview
@Composable
fun DayDetailSheetAddPreview() {
    PreviewDayDetail(
        events = previewEvents,
        adding = true,
        addTypes = AddEventTypesUi.Success(PreviewEventTypeSections),
    )
}

@Preview
@Composable
fun DayDetailSheetAddErrorPreview() {
    PreviewDayDetail(
        events = previewEvents,
        adding = true,
        addTypes = AddEventTypesUi.Error(AddEventTypesError.LoadFailed),
    )
}
