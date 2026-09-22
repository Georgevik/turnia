package com.geoviksoft.turnia.ui.components.daydetail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.geoviksoft.turnia.ui.components.daydetail.model.OneOffEventFormUi
import com.geoviksoft.turnia.ui.components.daydetail.model.OneOffFormAction
import com.geoviksoft.turnia.ui.system.PreviewTurniaTheme
import com.geoviksoft.turnia.ui.system.color.EntityPalette
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.color_selected
import turnia.app.shared.generated.resources.one_off_event_all_day
import turnia.app.shared.generated.resources.one_off_event_cancel
import turnia.app.shared.generated.resources.one_off_event_delete
import turnia.app.shared.generated.resources.one_off_event_end
import turnia.app.shared.generated.resources.one_off_event_end_before_start
import turnia.app.shared.generated.resources.one_off_event_name
import turnia.app.shared.generated.resources.one_off_event_notes
import turnia.app.shared.generated.resources.one_off_event_save
import turnia.app.shared.generated.resources.one_off_event_start

/** Long enough for the field to have grown into the form before the keyboard pushes the sheet up. */
private const val FOCUS_DELAY_MS = 320L

private enum class PickerTarget { Start, End }

@Composable
fun OneOffEventForm(
    form: OneOffEventFormUi,
    onAction: (OneOffFormAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val nameFocus = remember { FocusRequester() }
    var picking by remember { mutableStateOf<PickerTarget?>(null) }

    // Only a new event starts typing: an existing one is often opened to be read or deleted, and a
    // keyboard would cover half of it.
    LaunchedEffect(Unit) {
        if (form.editing != null) return@LaunchedEffect
        delay(FOCUS_DELAY_MS)
        nameFocus.requestFocus()
    }

    // Registered after the sheet's own, so it goes first: back closes the form, not the sheet.
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        onBackCompleted = { onAction(OneOffFormAction.Cancel) },
    )

    picking?.let { target ->
        DateTimePickerDialog(
            initial = if (target == PickerTarget.Start) form.start else form.end,
            pickTime = !form.allDay,
            onDismiss = { picking = null },
            onConfirm = { picked ->
                onAction(
                    if (target == PickerTarget.Start) OneOffFormAction.StartChanged(picked)
                    else OneOffFormAction.EndChanged(picked),
                )
                picking = null
            },
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.5.dp,  MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = form.name,
                onValueChange = { onAction(OneOffFormAction.NameChanged(it)) },
                label = { Text(stringResource(Res.string.one_off_event_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().focusRequester(nameFocus),
            )

            AllDayToggle(
                checked = form.allDay,
                onCheckedChange = { onAction(OneOffFormAction.AllDayChanged(it)) },
            )

            DateTimeField(
                label = stringResource(Res.string.one_off_event_start),
                value = form.start,
                allDay = form.allDay,
                onClick = { picking = PickerTarget.Start },
            )
            DateTimeField(
                label = stringResource(Res.string.one_off_event_end),
                value = form.end,
                allDay = form.allDay,
                isError = form.endsBeforeStart,
                onClick = { picking = PickerTarget.End },
            )
            if (form.endsBeforeStart) {
                Text(
                    text = stringResource(Res.string.one_off_event_end_before_start),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            ColorRow(
                colors = EntityPalette,
                selected = form.color,
                onPick = { onAction(OneOffFormAction.ColorPicked(it)) },
            )

            OutlinedTextField(
                value = form.notes,
                onValueChange = { onAction(OneOffFormAction.NotesChanged(it)) },
                label = { Text(stringResource(Res.string.one_off_event_notes)) },
                minLines = 2,
                keyboardOptions = KeyboardOptions.Default.copy(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (form.editing != null) {
                    TextButton(
                        onClick = { onAction(OneOffFormAction.Delete) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text(stringResource(Res.string.one_off_event_delete))
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { onAction(OneOffFormAction.Cancel) }) {
                    Text(stringResource(Res.string.one_off_event_cancel))
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onAction(OneOffFormAction.Save) },
                    enabled = form.canSave,
                ) {
                    Text(
                        text = stringResource(Res.string.one_off_event_save),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun DateTimeField(
    label: String,
    value: LocalDateTime,
    allDay: Boolean,
    onClick: () -> Unit,
    isError: Boolean = false,
) {
    val borderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (allDay) value.date.dateLabel() else value.dateTimeLabel(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Icon(
                imageVector = Icons.Outlined.Event,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AllDayToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.one_off_event_all_day),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        // The row takes the tap, so the switch is only there to be seen.
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** One scrolling row rather than a wrapping grid: the form has to stay short inside the sheet. */
@Composable
private fun ColorRow(
    colors: List<Color>,
    selected: Color,
    onPick: (Color) -> Unit,
) {
    val selectedLabel = stringResource(Res.string.color_selected)
    val ring = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        colors.forEach { color ->
            val isSelected = color.value == selected.value
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(30.dp)
                    .then(if (isSelected) Modifier.border(2.dp, ring, CircleShape) else Modifier)
                    .padding(4.dp)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onPick(color) },
                    ),
            ) {
                Surface(shape = CircleShape, color = color, modifier = Modifier.fillMaxSize()) {
                    if (isSelected) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = selectedLabel,
                                tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// Previews. The values are developer-facing, so they stay here rather than in composeResources.

private val previewStart = LocalDateTime(LocalDate(2026, 10, 3), LocalTime(17, 30))

@Preview
@Composable
fun OneOffEventFormPreview() {
    PreviewTurniaTheme {
        OneOffEventForm(
            form = OneOffEventFormUi(
                name = "Dentista",
                notes = "Llevar la tarjeta del seguro",
                start = previewStart,
                end = LocalDateTime(previewStart.date, LocalTime(18, 15)),
                color = EntityPalette.first(),
            ),
            onAction = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview
@Composable
fun OneOffEventFormEndsBeforeStartPreview() {
    PreviewTurniaTheme {
        OneOffEventForm(
            form = OneOffEventFormUi(
                name = "Dentista",
                start = previewStart,
                end = LocalDateTime(previewStart.date, LocalTime(16, 0)),
                color = EntityPalette[2],
            ),
            onAction = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview
@Composable
fun OneOffEventFormAllDayPreview() {
    PreviewTurniaTheme {
        OneOffEventForm(
            form = OneOffEventFormUi(
                name = "Congreso de enfermería",
                start = previewStart,
                end = LocalDateTime(LocalDate(2026, 10, 4), LocalTime(18, 15)),
                allDay = true,
                color = EntityPalette[3],
            ),
            onAction = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
