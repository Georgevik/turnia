package com.geoviksoft.turnia.ui.main.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.StarRate
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geoviksoft.turnia.navigation.LocalNavigator
import com.geoviksoft.turnia.ui.system.AppStore
import com.geoviksoft.turnia.ui.system.LocalSnackbar
import com.geoviksoft.turnia.ui.system.PreviewTurniaTheme
import com.geoviksoft.turnia.ui.system.components.TurniaLogo
import com.geoviksoft.turnia.ui.system.rememberAppVersion
import com.geoviksoft.turnia.ui.system.rememberStoreReview
import com.geoviksoft.turnia.ui.system.rememberTextSharer
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.about_feedback_diagnostics
import turnia.app.shared.generated.resources.about_feedback_no_email
import turnia.app.shared.generated.resources.about_feedback_rate_app_store
import turnia.app.shared.generated.resources.about_feedback_rate_play
import turnia.app.shared.generated.resources.about_feedback_report
import turnia.app.shared.generated.resources.about_feedback_suggest
import turnia.app.shared.generated.resources.about_privacy
import turnia.app.shared.generated.resources.about_section_account
import turnia.app.shared.generated.resources.about_section_feedback
import turnia.app.shared.generated.resources.about_section_legal
import turnia.app.shared.generated.resources.about_terms
import turnia.app.shared.generated.resources.about_title
import turnia.app.shared.generated.resources.about_user_id
import turnia.app.shared.generated.resources.about_user_id_copied
import turnia.app.shared.generated.resources.about_user_id_copy
import turnia.app.shared.generated.resources.about_version
import turnia.app.shared.generated.resources.app_name
import turnia.app.shared.generated.resources.calendar_back

@Composable
fun AboutScreen(viewModel: AboutViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbar.current
    val version = rememberAppVersion()

    AboutScreenContent(
        state = state,
        onFeedback = { kind -> viewModel.feedbackRequested(kind, version) },
    )

    state.feedbackMail?.let { mail ->
        FeedbackMailLauncher(
            mail = mail,
            onOpened = viewModel::feedbackMailOpened,
            onFailed = viewModel::feedbackMailFailed,
        )
    }

    state.userMessage?.let { message ->
        val text = when (message) {
            AboutMessage.NoEmailApp -> stringResource(Res.string.about_feedback_no_email, state.supportEmail)
        }
        LaunchedEffect(message) {
            snackbar.showSnackbar(text)
            viewModel.userMessageShown()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutScreenContent(state: AboutUi, onFeedback: (FeedbackKind) -> Unit) {
    val navigator = LocalNavigator.current
    val uriHandler = LocalUriHandler.current

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

            FeedbackSection(onFeedback = onFeedback)

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            SectionHeader(stringResource(Res.string.about_section_legal))
            val termsUrl = LegalLinks.terms
            val privacyUrl = LegalLinks.privacy
            LinkRow(Icons.Default.Description, stringResource(Res.string.about_terms)) {
                uriHandler.openUri(termsUrl)
            }
            LinkRow(Icons.Default.PrivacyTip, stringResource(Res.string.about_privacy)) {
                uriHandler.openUri(privacyUrl)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            SectionHeader(stringResource(Res.string.about_section_account))
            UserIdRow(state.userId)
        }
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
        TurniaLogo(Modifier.size(72.dp))
        Text(
            text = stringResource(Res.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(Res.string.about_version, version.name),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FeedbackSection(onFeedback: (FeedbackKind) -> Unit) {
    val storeReview = rememberStoreReview()

    SectionHeader(stringResource(Res.string.about_section_feedback))
    LinkRow(Icons.Default.BugReport, stringResource(Res.string.about_feedback_report)) {
        onFeedback(FeedbackKind.REPORT)
    }
    LinkRow(Icons.Default.Lightbulb, stringResource(Res.string.about_feedback_suggest)) {
        onFeedback(FeedbackKind.SUGGESTION)
    }
    LinkRow(
        icon = Icons.Default.StarRate,
        label = stringResource(
            when (storeReview.store) {
                AppStore.GOOGLE_PLAY -> Res.string.about_feedback_rate_play
                AppStore.APP_STORE -> Res.string.about_feedback_rate_app_store
            }
        ),
        onClick = storeReview::open,
    )
}

/** Resolves the mail the ViewModel asked for and hands it to the mail app. */
@Composable
private fun FeedbackMailLauncher(mail: FeedbackMailUi, onOpened: () -> Unit, onFailed: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val subject = stringResource(mail.subject)
    val prompt = stringResource(mail.prompt)
    val diagnostics = stringResource(
        Res.string.about_feedback_diagnostics,
        mail.versionName,
        mail.versionBuild,
        mail.system,
        mail.userId,
    )

    LaunchedEffect(mail) {
        val uri = FeedbackMail.uri(
            address = mail.address,
            subject = subject,
            body = "$prompt\n\n\n\n—\n$diagnostics",
        )
        try {
            uriHandler.openUri(uri)
            onOpened()
        } catch (_: IllegalArgumentException) {
            // Thrown on Android when nothing on the device handles mailto.
            onFailed()
        }
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

@Preview
@Composable
fun AboutScreenPreview() {
    PreviewTurniaTheme {
        AboutScreenContent(
            state = AboutUi(userId = "georgeclinton@my-own-personal-domain.com"),
            onFeedback = {},
        )
    }
}
