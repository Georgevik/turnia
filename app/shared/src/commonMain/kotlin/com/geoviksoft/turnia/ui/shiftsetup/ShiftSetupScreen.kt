package com.geoviksoft.turnia.ui.shiftsetup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.geoviksoft.turnia.core.domain.model.ShiftSetupVia
import com.geoviksoft.turnia.navigation.LocalNavigator
import com.geoviksoft.turnia.ui.system.LocalSnackbar
import com.geoviksoft.turnia.ui.system.TestTags
import com.geoviksoft.turnia.ui.system.color.toComposeColorOrNull
import com.geoviksoft.turnia.ui.system.components.AcronymBadge
import com.geoviksoft.turnia.ui.system.components.time.TTimeField
import com.geoviksoft.turnia.ui.system.keyboardAware
import com.geoviksoft.turnia.ui.system.toErrorSnackbar
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.calendar_back
import turnia.app.shared.generated.resources.event_type_field_acronym
import turnia.app.shared.generated.resources.event_type_field_end
import turnia.app.shared.generated.resources.event_type_field_name
import turnia.app.shared.generated.resources.event_type_field_start
import turnia.app.shared.generated.resources.shift_setup_add_custom
import turnia.app.shared.generated.resources.shift_setup_confirm
import turnia.app.shared.generated.resources.shift_setup_custom_add
import turnia.app.shared.generated.resources.shift_setup_custom_cancel
import turnia.app.shared.generated.resources.shift_setup_footer
import turnia.app.shared.generated.resources.shift_setup_required
import turnia.app.shared.generated.resources.shift_setup_skip
import turnia.app.shared.generated.resources.shift_setup_subtitle
import turnia.app.shared.generated.resources.shift_setup_title

@Composable
fun ShiftSetupScreen(via: ShiftSetupVia, viewModel: ShiftSetupViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    val snackbar = LocalSnackbar.current

    // Resolved here, in the language on screen: that is the one the created types keep.
    val presetText = ShiftPreset.entries.associateWith { preset ->
        stringResource(preset.title) to stringResource(preset.acronym)
    }

    LaunchedEffect(state.closed) {
        if (state.closed) navigator.goBack()
    }

    state.userMessage?.let { message ->
        val text = stringResource(message)
        LaunchedEffect(message) {
            snackbar.showSnackbar(text.toErrorSnackbar())
            viewModel.userMessageShown()
        }
    }

    // After sign-in, leaving is skipping. From the add pane it only closes what the user opened.
    val leave = if (via == ShiftSetupVia.Onboarding) viewModel::skip else navigator::goBack
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        onBackCompleted = { leave() },
    )

    Scaffold(
        modifier = Modifier.testTag(TestTags.SHIFT_SETUP),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    if (via == ShiftSetupVia.AddPane) {
                        IconButton(onClick = navigator::goBack) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(Res.string.calendar_back))
                        }
                    }
                },
                actions = {
                    if (via == ShiftSetupVia.Onboarding) {
                        TextButton(onClick = viewModel::skip) { Text(stringResource(Res.string.shift_setup_skip)) }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .keyboardAware()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(Res.string.shift_setup_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(Res.string.shift_setup_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))

            state.rows.forEachIndexed { index, row ->
                val (name, acronym) = row.preset?.let { presetText.getValue(it) } ?: (row.name to row.acronym)
                ShiftRow(
                    index = index,
                    row = row,
                    name = name,
                    acronym = acronym,
                    onToggle = { viewModel.toggle(row.id) },
                    onStartChange = { viewModel.startChanged(row.id, it) },
                    onEndChange = { viewModel.endChanged(row.id, it) },
                )
            }

            val custom = state.custom
            if (custom == null) {
                TextButton(onClick = viewModel::openCustom) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(Res.string.shift_setup_add_custom))
                }
            } else {
                CustomShift(
                    form = custom,
                    onChange = viewModel::customChanged,
                    onAdd = viewModel::addCustom,
                    onCancel = viewModel::cancelCustom,
                )
            }

            Text(
                text = stringResource(Res.string.shift_setup_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = { viewModel.confirm(presetText) },
                enabled = state.canConfirm,
                shape = RoundedCornerShape(percent = 50),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag(TestTags.SHIFT_SETUP_CONFIRM),
            ) {
                if (state.saving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text(
                        text = pluralStringResource(Res.plurals.shift_setup_confirm, state.selectedCount, state.selectedCount),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ShiftRow(
    index: Int,
    row: ShiftRowUi,
    name: String,
    acronym: String,
    onToggle: () -> Unit,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().testTag(TestTags.shiftSetupRow(index))) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Checkbox(checked = row.selected, onCheckedChange = { onToggle() })
            AcronymBadge(color = row.color.toComposeColorOrNull() ?: Color.Gray, acronym = acronym, size = 36.dp)
            Text(text = name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        }
        if (row.selected) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TTimeField(
                    value = row.start,
                    onValueChange = onStartChange,
                    label = stringResource(Res.string.event_type_field_start),
                    modifier = Modifier.weight(1f).testTag(TestTags.shiftSetupStart(index)),
                )
                TTimeField(
                    value = row.end,
                    onValueChange = onEndChange,
                    label = stringResource(Res.string.event_type_field_end),
                    modifier = Modifier.weight(1f).testTag(TestTags.shiftSetupEnd(index)),
                )
            }
        }
    }
}

@Composable
private fun CustomShift(
    form: CustomShiftForm,
    onChange: (CustomShiftForm) -> Unit,
    onAdd: () -> Unit,
    onCancel: () -> Unit,
) {
    val required = stringResource(Res.string.shift_setup_required)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = form.name,
                onValueChange = { onChange(form.copy(name = it)) },
                label = { Text(stringResource(Res.string.event_type_field_name)) },
                isError = form.nameMissing,
                supportingText = if (form.nameMissing) ({ Text(required) }) else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.weight(2f).testTag(TestTags.SHIFT_SETUP_CUSTOM_NAME),
            )
            OutlinedTextField(
                value = form.acronym,
                onValueChange = { onChange(form.copy(acronym = it)) },
                label = { Text(stringResource(Res.string.event_type_field_acronym)) },
                isError = form.acronymMissing,
                supportingText = if (form.acronymMissing) ({ Text(required) }) else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.weight(1f).testTag(TestTags.SHIFT_SETUP_CUSTOM_ACRONYM),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TTimeField(
                value = form.start,
                onValueChange = { onChange(form.copy(start = it)) },
                label = stringResource(Res.string.event_type_field_start),
                modifier = Modifier.weight(1f).testTag(TestTags.SHIFT_SETUP_CUSTOM_START),
            )
            TTimeField(
                value = form.end,
                onValueChange = { onChange(form.copy(end = it)) },
                label = stringResource(Res.string.event_type_field_end),
                modifier = Modifier.weight(1f).testTag(TestTags.SHIFT_SETUP_CUSTOM_END),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.End)) {
            TextButton(onClick = onCancel) { Text(stringResource(Res.string.shift_setup_custom_cancel)) }
            Button(onClick = onAdd) { Text(stringResource(Res.string.shift_setup_custom_add)) }
        }
    }
}
