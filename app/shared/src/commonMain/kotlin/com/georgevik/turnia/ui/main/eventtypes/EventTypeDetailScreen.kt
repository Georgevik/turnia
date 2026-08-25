package com.georgevik.turnia.ui.main.eventtypes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.georgevik.turnia.navigation.EventTypeKind
import com.georgevik.turnia.ui.main.eventtypes.components.ColorSwatchPicker
import com.georgevik.turnia.ui.main.eventtypes.model.EventTypeDetailUi
import com.georgevik.turnia.ui.system.components.AcronymBadge
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.calendar_back
import turnia.app.shared.generated.resources.event_type_detail_new_personal
import turnia.app.shared.generated.resources.event_type_detail_title_group
import turnia.app.shared.generated.resources.event_type_detail_title_personal
import turnia.app.shared.generated.resources.event_type_field_acronym
import turnia.app.shared.generated.resources.event_type_field_color
import turnia.app.shared.generated.resources.event_type_field_description
import turnia.app.shared.generated.resources.event_type_field_end
import turnia.app.shared.generated.resources.event_type_field_name
import turnia.app.shared.generated.resources.event_type_field_start
import turnia.app.shared.generated.resources.event_type_group_readonly
import turnia.app.shared.generated.resources.event_type_not_set
import turnia.app.shared.generated.resources.event_type_save
import turnia.app.shared.generated.resources.event_type_swap_allowed
import turnia.app.shared.generated.resources.event_type_swap_not_allowed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventTypeDetailScreen(
    viewModel: EventTypeDetailViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val title = when {
        uiState.kind == EventTypeKind.GROUP -> stringResource(Res.string.event_type_detail_title_group)
        uiState.isCreate -> stringResource(Res.string.event_type_detail_new_personal)
        else -> stringResource(Res.string.event_type_detail_title_personal)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(Res.string.calendar_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (uiState.fieldsEditable) {
                PersonalForm(uiState, viewModel)
            } else {
                GroupDetail(uiState)
            }

            FieldLabel(stringResource(Res.string.event_type_field_color))
            ColorSwatchPicker(
                selected = uiState.color,
                onPick = viewModel::onPickColor,
                modifier = Modifier.fillMaxWidth(),
            )

            if (uiState.fieldsEditable) {
                Button(
                    onClick = { if (viewModel.onSave()) onBack() },
                    enabled = uiState.name.isNotBlank(),
                    shape = RoundedCornerShape(percent = 50),
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
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
private fun ColumnScope.GroupDetail(uiState: EventTypeDetailUi) {
    // Identity header: the color badge + name so the chosen color reads at a glance.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AcronymBadge(color = uiState.color, acronym = uiState.acronym.ifBlank { null }, size = 52.dp)
        Column {
            Text(
                text = uiState.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (uiState.acronym.isNotBlank()) {
                Text(
                    text = uiState.acronym,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    ReadOnlyField(
        stringResource(Res.string.event_type_field_description),
        uiState.description,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ReadOnlyField(
            stringResource(Res.string.event_type_field_start),
            uiState.startTime,
            modifier = Modifier.weight(1f),
        )
        ReadOnlyField(
            stringResource(Res.string.event_type_field_end),
            uiState.endTime,
            modifier = Modifier.weight(1f),
        )
    }

    uiState.swappable?.let { SwapBadge(allowed = it) }

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
private fun ColumnScope.PersonalForm(
    uiState: EventTypeDetailUi,
    viewModel: EventTypeDetailViewModel,
) {
    OutlinedTextField(
        value = uiState.name,
        onValueChange = viewModel::onNameChange,
        label = { Text(stringResource(Res.string.event_type_field_name)) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = uiState.acronym,
        onValueChange = viewModel::onAcronymChange,
        label = { Text(stringResource(Res.string.event_type_field_acronym)) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = uiState.description,
        onValueChange = viewModel::onDescriptionChange,
        label = { Text(stringResource(Res.string.event_type_field_description)) },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = uiState.startTime,
            onValueChange = viewModel::onStartTimeChange,
            label = { Text(stringResource(Res.string.event_type_field_start)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = uiState.endTime,
            onValueChange = viewModel::onEndTimeChange,
            label = { Text(stringResource(Res.string.event_type_field_end)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f),
        )
    }
}

/** Uppercase, letter-spaced monospace section label — the image's field-label style. */
@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Read-only value rendered like a disabled form field: label above, boxed value. */
@Composable
private fun ReadOnlyField(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(label)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = value.ifBlank { stringResource(Res.string.event_type_not_set) },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            )
        }
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
