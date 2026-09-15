package com.geoviksoft.turnia.ui.signin.createaccount

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geoviksoft.turnia.core.domain.model.PASSWORD_MIN_LENGTH
import com.geoviksoft.turnia.core.domain.model.PasswordRule
import com.geoviksoft.turnia.navigation.LocalNavigator
import com.geoviksoft.turnia.ui.main.about.LegalLinks
import com.geoviksoft.turnia.ui.signin.components.PasswordField
import com.geoviksoft.turnia.ui.system.PreviewTurniaTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.about_privacy
import turnia.app.shared.generated.resources.about_terms
import turnia.app.shared.generated.resources.calendar_back
import turnia.app.shared.generated.resources.create_account_accept_privacy
import turnia.app.shared.generated.resources.create_account_accept_terms
import turnia.app.shared.generated.resources.create_account_body
import turnia.app.shared.generated.resources.create_account_password_rule_length
import turnia.app.shared.generated.resources.create_account_password_rule_lowercase
import turnia.app.shared.generated.resources.create_account_password_rule_uppercase
import turnia.app.shared.generated.resources.create_account_password_rules_title
import turnia.app.shared.generated.resources.profile_field_email
import turnia.app.shared.generated.resources.profile_field_name
import turnia.app.shared.generated.resources.signin_email_create_account
import turnia.app.shared.generated.resources.signin_email_error_email_in_use
import turnia.app.shared.generated.resources.signin_email_error_invalid_email
import turnia.app.shared.generated.resources.signin_email_error_weak_password
import turnia.app.shared.generated.resources.signin_error_failed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAccountScreen(viewModel: CreateAccountViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CreateAccountScreenContent(state, viewModel::onFieldChanged, viewModel::onSubmit)
}

@Composable
fun CreateAccountScreenContent(
    state: CreateAccountUi,
    onFieldChanged: (CreateAccountField, Any) -> Unit,
    onSubmit: () -> Unit
) {
    val navigator = LocalNavigator.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.signin_email_create_account)) },
                navigationIcon = {
                    IconButton(onClick = navigator::goBack, enabled = !state.submitting) {
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(Res.string.create_account_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = state.name,
                onValueChange = { onFieldChanged(CreateAccountField.Name, it) },
                label = { Text(stringResource(Res.string.profile_field_name)) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                singleLine = true,
                enabled = !state.submitting,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
                    .semantics { contentType = ContentType.PersonFullName },
            )

            val emailError = when (state.error) {
                CreateAccountError.InvalidEmail -> Res.string.signin_email_error_invalid_email
                CreateAccountError.EmailInUse -> Res.string.signin_email_error_email_in_use
                else -> null
            }
            OutlinedTextField(
                value = state.email,
                onValueChange = { onFieldChanged(CreateAccountField.Email, it) },
                label = { Text(stringResource(Res.string.profile_field_email)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                enabled = !state.submitting,
                isError = emailError != null,
                supportingText = emailError?.let { { Text(stringResource(it)) } },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
                    .semantics { contentType = ContentType.EmailAddress },
            )

            PasswordField(
                value = state.password,
                onValueChange = { onFieldChanged(CreateAccountField.Password, it) },
                onDone = onSubmit,
                enabled = !state.submitting,
                isNewPassword = true,
                errorText = if (state.error == CreateAccountError.WeakPassword) {
                    stringResource(Res.string.signin_email_error_weak_password)
                } else null,
                modifier = Modifier.fillMaxWidth(),
            )

            PasswordRules(metRules = state.metRules)

            Column {
                LegalCheckbox(
                    checked = state.termsAccepted,
                    onCheckedChange = { onFieldChanged(CreateAccountField.Terms, it) },
                    text = Res.string.create_account_accept_terms,
                    link = Res.string.about_terms,
                    url = LegalLinks.TERMS,
                    enabled = !state.submitting,
                )
                LegalCheckbox(
                    checked = state.privacyAccepted,
                    onCheckedChange = { onFieldChanged(CreateAccountField.Privacy, it) },
                    text = Res.string.create_account_accept_privacy,
                    link = Res.string.about_privacy,
                    url = LegalLinks.PRIVACY,
                    enabled = !state.submitting,
                )

            }

            if (state.error == CreateAccountError.Failed) {
                Text(
                    text = stringResource(Res.string.signin_error_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onSubmit,
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (state.submitting) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(Res.string.signin_email_create_account))
                }
            }
        }
    }
}

/**
 * "I accept the [link]": the whole row toggles the box, and only the linked words open the page, so
 * reading the terms never ticks them by accident.
 */
@Composable
private fun LegalCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: StringResource,
    link: StringResource,
    url: String,
    enabled: Boolean,
) {
    val uriHandler = LocalUriHandler.current
    val linkText = stringResource(link)
    val fullText = stringResource(text, linkText)
    val linkColor = MaterialTheme.colorScheme.primary
    val annotated = buildAnnotatedString {
        val start = fullText.indexOf(linkText)
        if (start < 0) {
            append(fullText)
            return@buildAnnotatedString
        }
        append(fullText.substring(0, start))
        withLink(
            LinkAnnotation.Clickable(
                tag = url,
                styles = TextLinkStyles(
                    SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline
                    )
                ),
                linkInteractionListener = { uriHandler.openUri(url) },
            )
        ) { append(linkText) }
        append(fullText.substring(start + linkText.length))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Its own 48dp touch target would indent it from the fields above; the row is the target.
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = annotated,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Ticked off as the password is typed, so the rules are known before the button refuses it. */
@Composable
private fun PasswordRules(metRules: Set<PasswordRule>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.create_account_password_rules_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PasswordRule.entries.forEach { rule ->
            val met = rule in metRules
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = if (met) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = null,
                    tint = if (met) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = when (rule) {
                        PasswordRule.MinLength -> stringResource(
                            Res.string.create_account_password_rule_length,
                            PASSWORD_MIN_LENGTH
                        )

                        PasswordRule.Uppercase -> stringResource(Res.string.create_account_password_rule_uppercase)
                        PasswordRule.Lowercase -> stringResource(Res.string.create_account_password_rule_lowercase)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (met) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview
@Composable
fun CreateAccountScreenPreview() {
    PreviewTurniaTheme {
        CreateAccountScreenContent(
            state = CreateAccountUi(),
            onFieldChanged = { _, _ -> },
            onSubmit = {},
        )
    }
}
