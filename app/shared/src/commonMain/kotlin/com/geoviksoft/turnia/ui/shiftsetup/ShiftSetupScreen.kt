package com.geoviksoft.turnia.ui.shiftsetup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.geoviksoft.turnia.core.domain.model.ShiftSetupVia
import com.geoviksoft.turnia.navigation.LocalNavigator
import com.geoviksoft.turnia.ui.system.LocalSnackbar
import com.geoviksoft.turnia.ui.system.TestTags
import com.geoviksoft.turnia.ui.system.keyboardAware
import com.geoviksoft.turnia.ui.system.toErrorSnackbar
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.calendar_back
import turnia.app.shared.generated.resources.shift_setup_add_custom
import turnia.app.shared.generated.resources.shift_setup_confirm
import turnia.app.shared.generated.resources.shift_setup_footer
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
                CreateInlineShiftRow(
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
