package com.geoviksoft.turnia.ui.main.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geoviksoft.turnia.core.domain.model.DeleteAccountError
import com.geoviksoft.turnia.navigation.LocalNavigator
import com.geoviksoft.turnia.ui.system.LocalSnackbar
import com.geoviksoft.turnia.ui.system.components.ConfirmationDialog
import com.geoviksoft.turnia.ui.system.components.ConfirmationStatus
import com.geoviksoft.turnia.ui.system.rememberAppVersion
import com.geoviksoft.turnia.ui.system.rememberTextSharer
import com.geoviksoft.turnia.ui.system.toErrorSnackbar
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.about_delete_account
import turnia.app.shared.generated.resources.about_delete_account_confirm
import turnia.app.shared.generated.resources.about_delete_account_error
import turnia.app.shared.generated.resources.about_delete_account_error_last_admin
import turnia.app.shared.generated.resources.about_delete_account_message
import turnia.app.shared.generated.resources.about_delete_account_title
import turnia.app.shared.generated.resources.about_privacy
import turnia.app.shared.generated.resources.about_privacy_path
import turnia.app.shared.generated.resources.about_section_account
import turnia.app.shared.generated.resources.about_section_legal
import turnia.app.shared.generated.resources.about_title
import turnia.app.shared.generated.resources.about_user_id
import turnia.app.shared.generated.resources.about_user_id_copied
import turnia.app.shared.generated.resources.about_user_id_copy
import turnia.app.shared.generated.resources.about_version
import turnia.app.shared.generated.resources.app_name
import turnia.app.shared.generated.resources.calendar_back
import turnia.app.shared.generated.resources.dialog_cancel
import turnia.app.shared.generated.resources.logo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(viewModel: AboutViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    val snackbar = LocalSnackbar.current
    val uriHandler = LocalUriHandler.current
    var confirmingDelete by remember { mutableStateOf(false) }

    state.userMessage?.let { error ->
        val text = stringResource(error.message())
        LaunchedEffect(error) {
            snackbar.showSnackbar(text.toErrorSnackbar())
            viewModel.userMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.about_title)) },
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
            AppHeader()

            SectionHeader(stringResource(Res.string.about_section_legal))
            val privacyUrl = LegalLinks.privacy(stringResource(Res.string.about_privacy_path))
            LinkRow(Icons.Default.PrivacyTip, stringResource(Res.string.about_privacy)) {
                uriHandler.openUri(privacyUrl)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            SectionHeader(stringResource(Res.string.about_section_account))
            UserIdRow(state.userId)
            DeleteAccountRow(onClick = { confirmingDelete = true })
        }
    }

    // Kept up while the deletion runs, spinner and all: closing it would suggest it was over.
    if (confirmingDelete || state.deletingAccount) {
        ConfirmationDialog(
            title = stringResource(Res.string.about_delete_account_title),
            message = stringResource(Res.string.about_delete_account_message),
            confirmText = stringResource(Res.string.about_delete_account_confirm),
            dismissText = stringResource(Res.string.dialog_cancel),
            onConfirm = {
                confirmingDelete = false
                viewModel.onDeleteAccountConfirmed()
            },
            onDismissRequest = { confirmingDelete = false },
            status = if (state.deletingAccount) ConfirmationStatus.Running else ConfirmationStatus.Idle,
        )
    }
}

@Composable
private fun AppHeader() {
    val version = rememberAppVersion()

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.logo),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer),
                modifier = Modifier.size(44.dp),
            )
        }
        Text(
            text = stringResource(Res.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(Res.string.about_version, version.name, version.build),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun LinkRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** What support asks for to find an account: the one identifier that survives a rename. */
@Composable
private fun UserIdRow(userId: String) {
    val sharer = rememberTextSharer()
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val copied = stringResource(Res.string.about_user_id_copied)
    val copy = {
        sharer.copy(userId)
        scope.launch { snackbar.showSnackbar(copied) }
        Unit
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = userId.isNotEmpty(), onClick = copy)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Fingerprint,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.about_user_id),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = userId,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = copy, enabled = userId.isNotEmpty()) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = stringResource(Res.string.about_user_id_copy),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun DeleteAccountRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.DeleteForever,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
        )
        Text(
            text = stringResource(Res.string.about_delete_account),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

private fun DeleteAccountError.message() = when (this) {
    DeleteAccountError.LastAdmin -> Res.string.about_delete_account_error_last_admin
    DeleteAccountError.Failed -> Res.string.about_delete_account_error
}
