package com.geoviksoft.turnia.ui.main.preferences

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geoviksoft.turnia.navigation.LocalNavigator
import com.geoviksoft.turnia.ui.main.preferences.components.LanguageFlag
import com.geoviksoft.turnia.ui.main.preferences.components.LanguageSheet
import com.geoviksoft.turnia.ui.main.preferences.components.label
import com.geoviksoft.turnia.ui.system.AppLanguage
import com.geoviksoft.turnia.ui.system.LocalSnackbar
import com.geoviksoft.turnia.ui.system.toErrorSnackbar
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.calendar_back
import turnia.app.shared.generated.resources.notifications_enable
import turnia.app.shared.generated.resources.notifications_enable_description
import turnia.app.shared.generated.resources.notifications_save_error
import turnia.app.shared.generated.resources.notifications_system_hint
import turnia.app.shared.generated.resources.preferences_section_language
import turnia.app.shared.generated.resources.preferences_section_notifications
import turnia.app.shared.generated.resources.preferences_title

/**
 * Whether this account wants push at all, and which language the app speaks.
 *
 * The switch is the account's answer, not the system permission — the OS keeps its own, and only it
 * can change that one — so the screen says as much rather than letting a user who denied
 * notifications wonder why an enabled switch tells them nothing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(viewModel: PreferencesViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    val snackbar = LocalSnackbar.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Not saveable: applying a language recreates the screen, and the sheet should not come back.
    var languageSheetOpen by remember { mutableStateOf(false) }

    state.userMessage?.let { message ->
        val text = stringResource(Res.string.notifications_save_error)
        LaunchedEffect(message) {
            snackbar.showSnackbar(text.toErrorSnackbar())
            viewModel.userMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.preferences_title)) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            SectionHeader(stringResource(Res.string.preferences_section_notifications))
            EnableRow(
                enabled = state.notificationsEnabled,
                onEnabledChange = viewModel::onEnabledChanged,
            )
            Text(
                text = stringResource(Res.string.notifications_system_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            SectionHeader(stringResource(Res.string.preferences_section_language))
            LanguageRow(language = state.language, onClick = { languageSheetOpen = true })
        }
    }

    if (languageSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { languageSheetOpen = false },
            sheetState = sheetState,
        ) {
            LanguageSheet(
                selected = state.language,
                onSelect = { language ->
                    languageSheetOpen = false
                    viewModel.onLanguageSelected(language)
                },
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

/** The whole row toggles: a switch on its own is a small target. */
@Composable
private fun EnableRow(enabled: Boolean, onEnabledChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEnabledChange(!enabled) }
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.notifications_enable),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(Res.string.notifications_enable_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = enabled, onCheckedChange = onEnabledChange)
    }
}

@Composable
private fun LanguageRow(language: AppLanguage, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LanguageFlag(language)
        Text(
            text = stringResource(language.label),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
