package com.georgevik.turnia.ui.main.eventtypes.detail

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.georgevik.turnia.navigation.LocalNavigator
import com.georgevik.turnia.ui.components.daydetail.components.EventTypeChip
import com.georgevik.turnia.ui.main.eventtypes.detail.model.EventTypeDetailUi
import com.georgevik.turnia.ui.main.eventtypes.detail.model.EventTypeDetailUi.EventTypeForm
import com.georgevik.turnia.ui.main.eventtypes.detail.model.EventTypeDetailUi.FormErrors
import com.georgevik.turnia.ui.main.eventtypes.detail.model.EventTypeFieldError
import com.georgevik.turnia.ui.main.eventtypes.detail.model.EventTypeScreenError
import com.georgevik.turnia.ui.main.eventtypes.detail.model.EventTypeTitle
import com.georgevik.turnia.ui.main.eventtypes.detail.model.EventTypeToastError
import com.georgevik.turnia.ui.system.LocalSnackbar
import com.georgevik.turnia.ui.system.components.AcronymBadge
import com.georgevik.turnia.ui.system.components.ColorSwatchPicker
import com.georgevik.turnia.ui.system.components.TFieldLabel
import com.georgevik.turnia.ui.system.components.TReadOnlyField
import com.georgevik.turnia.ui.system.components.TurniaDialogError
import com.georgevik.turnia.ui.system.components.time.TTimeField
import com.georgevik.turnia.ui.system.toErrorSnackbar
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.calendar_back
import turnia.app.shared.generated.resources.event_details_new_title
import turnia.app.shared.generated.resources.event_type_error_group_not_found
import turnia.app.shared.generated.resources.event_type_error_not_implemented
import turnia.app.shared.generated.resources.event_type_error_pick_color
import turnia.app.shared.generated.resources.event_type_error_required
import turnia.app.shared.generated.resources.event_type_error_save_group
import turnia.app.shared.generated.resources.event_type_error_save_personal
import turnia.app.shared.generated.resources.event_type_error_type_not_found
import turnia.app.shared.generated.resources.event_type_field_acronym
import turnia.app.shared.generated.resources.event_type_field_color
import turnia.app.shared.generated.resources.event_type_field_description
import turnia.app.shared.generated.resources.event_type_field_end
import turnia.app.shared.generated.resources.event_type_field_name
import turnia.app.shared.generated.resources.event_type_field_start
import turnia.app.shared.generated.resources.event_type_group_readonly
import turnia.app.shared.generated.resources.event_type_save
import turnia.app.shared.generated.resources.event_type_swap_allowed
import turnia.app.shared.generated.resources.event_type_swap_not_allowed

@Composable
fun EventTypeDetailScreen(viewModel: EventTypeDetailViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    val snackbar = LocalSnackbar.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when (val state = uiState) {
                        EventTypeDetailUi.Loading,
                        is EventTypeDetailUi.Error -> Unit

                        is EventTypeDetailUi.Success -> Text(
                            text = when (state.title) {
                                is EventTypeTitle.Title -> state.title.title
                                is EventTypeTitle.New -> stringResource(Res.string.event_details_new_title)
                            },
                            overflow = TextOverflow.Ellipsis
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
        when (val state = uiState) {
            EventTypeDetailUi.Loading -> LoadingContent(
                Modifier.fillMaxSize().padding(innerPadding)
            )

            is EventTypeDetailUi.Success -> {
                if (state.toastError != null) {
                    val message = state.toastError.message()
                    LaunchedEffect(message) {
                        snackbar.showSnackbar(message.toErrorSnackbar())
                        viewModel.hideMessageError()
                    }
                }
                LaunchedEffect(state.isSaved) {
                    if (state.isSaved) navigator.goBack()
                }
                EventTypeFormContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(20.dp),
                    state = state,
                    onSave = viewModel::onSave,
                    onPickColor = viewModel::onPickColor,
                    onFieldChanged = viewModel::onFieldChanged
                )
            }

            is EventTypeDetailUi.Error -> TurniaDialogError(
                message = state.error.message(),
                onDismiss = navigator::goBack,
            )
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
    }
}


@Composable
private fun EventTypeFormContent(
    modifier: Modifier = Modifier,
    state: EventTypeDetailUi.Success,
    onSave: () -> Unit,
    onPickColor: (color: Color) -> Unit,
    onFieldChanged: (EventTypeField, String) -> Unit,
) {
    val eventTypeForm = state.form
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (eventTypeForm.fieldsEditable) {
            PersonalForm(eventTypeForm, state.formErrors, onFieldChanged)
        } else {
            GroupDetail(eventTypeForm)
        }

        TFieldLabel(stringResource(Res.string.event_type_field_color))
        ColorSwatchPicker(
            colors = state.colors,
            selected = eventTypeForm.color,
            onPick = onPickColor,
            modifier = Modifier.fillMaxWidth(),
        )

        if (eventTypeForm.fieldsEditable) {
            Button(
                onClick = onSave,
                enabled = !state.saveButtonLoading,
                shape = RoundedCornerShape(percent = 50),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {

                if (state.saveButtonLoading) {
                    CircularProgressIndicator()
                } else {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(Res.string.event_type_save),
                        modifier = Modifier.padding(start = 8.dp),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupDetail(ui: EventTypeForm) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AcronymBadge(
            color = ui.color,
            acronym = ui.acronym.ifBlank { null },
            size = 52.dp
        )
        Column {
            Text(
                text = ui.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (ui.acronym.isNotBlank()) {
                Text(
                    text = ui.acronym,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    TReadOnlyField(
        stringResource(Res.string.event_type_field_description),
        ui.description,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TReadOnlyField(
            stringResource(Res.string.event_type_field_start),
            ui.startTime,
            modifier = Modifier.weight(1f),
        )
        TReadOnlyField(
            stringResource(Res.string.event_type_field_end),
            ui.endTime,
            modifier = Modifier.weight(1f),
        )
    }

    ui.swappable?.let { SwapBadge(allowed = it) }

    // Subtle caption clarifying only the color is the user's to change.
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
            text = stringResource(Res.string.event_type_group_readonly),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PersonalForm(
    ui: EventTypeForm,
    errors: FormErrors,
    onFieldChanged: (EventTypeField, String) -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        EventTypeChip(chipUi = ui.chipUi)
    }
    OutlinedTextField(
        value = ui.name,
        onValueChange = { onFieldChanged(EventTypeField.Name, it) },
        label = { Text(stringResource(Res.string.event_type_field_name)) },
        keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Words),
        singleLine = true,
        isError = errors.nameError != null,
        supportingText = errors.nameError?.let { { Text(it.message()) } },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = ui.acronym,
        onValueChange = { onFieldChanged(EventTypeField.Acronym, it.uppercase().take(4)) },
        label = { Text(stringResource(Res.string.event_type_field_acronym)) },
        singleLine = true,
        isError = errors.acronymError != null,
        supportingText = errors.acronymError?.let { { Text(it.message()) } },
        keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Characters),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = ui.description,
        onValueChange = { onFieldChanged(EventTypeField.Description, it) },
        keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Sentences),
        label = { Text(stringResource(Res.string.event_type_field_description)) },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TTimeField(
            value = ui.startTime,
            onValueChange = { onFieldChanged(EventTypeField.StartTime, it) },
            label = stringResource(Res.string.event_type_field_start),
            modifier = Modifier.weight(1f),
        )
        TTimeField(
            value = ui.endTime,
            onValueChange = { onFieldChanged(EventTypeField.EndTime, it) },
            label = stringResource(Res.string.event_type_field_end),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SwapBadge(allowed: Boolean) {
    val label = if (allowed) stringResource(Res.string.event_type_swap_allowed)
    else stringResource(Res.string.event_type_swap_not_allowed)
    val container = if (allowed) MaterialTheme.colorScheme.secondaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
    val content = if (allowed) MaterialTheme.colorScheme.onSecondaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = container,
        contentColor = content,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = if (allowed) Icons.Default.SwapHoriz else Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun EventTypeScreenError.message(): String = stringResource(
    when (this) {
        EventTypeScreenError.GroupNotFound -> Res.string.event_type_error_group_not_found
        EventTypeScreenError.GroupEventNotFound -> Res.string.event_type_error_type_not_found
    }
)

@Composable
private fun EventTypeFieldError.message(): String = stringResource(
    when (this) {
        EventTypeFieldError.Required -> Res.string.event_type_error_required
    }
)

@Composable
private fun EventTypeToastError.message(): String = stringResource(
    when (this) {
        EventTypeToastError.PickColor -> Res.string.event_type_error_pick_color
        EventTypeToastError.SavePersonal -> Res.string.event_type_error_save_personal
        EventTypeToastError.SaveGroup -> Res.string.event_type_error_save_group
        EventTypeToastError.NotImplemented -> Res.string.event_type_error_not_implemented
    }
)
