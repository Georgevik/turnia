package com.geoviksoft.turnia.ui.signin.createaccount

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geoviksoft.turnia.core.domain.model.PASSWORD_MIN_LENGTH
import com.geoviksoft.turnia.core.domain.model.PasswordRule
import com.geoviksoft.turnia.navigation.LocalNavigator
import com.geoviksoft.turnia.ui.signin.components.PasswordField
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.calendar_back
import turnia.app.shared.generated.resources.create_account_body
import turnia.app.shared.generated.resources.create_account_password_rule_length
import turnia.app.shared.generated.resources.create_account_password_rule_lowercase
import turnia.app.shared.generated.resources.create_account_password_rule_uppercase
import turnia.app.shared.generated.resources.create_account_password_rules_title
import turnia.app.shared.generated.resources.profile_field_email
import turnia.app.shared.generated.resources.signin_email_create_account
import turnia.app.shared.generated.resources.signin_email_error_email_in_use
import turnia.app.shared.generated.resources.signin_email_error_invalid_email
import turnia.app.shared.generated.resources.signin_email_error_weak_password
import turnia.app.shared.generated.resources.signin_error_failed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAccountScreen(viewModel: CreateAccountViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
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

            val emailError = when (state.error) {
                CreateAccountError.InvalidEmail -> Res.string.signin_email_error_invalid_email
                CreateAccountError.EmailInUse -> Res.string.signin_email_error_email_in_use
                else -> null
            }
            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChanged,
                label = { Text(stringResource(Res.string.profile_field_email)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                singleLine = true,
                enabled = !state.submitting,
                isError = emailError != null,
                supportingText = emailError?.let { { Text(stringResource(it)) } },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.EmailAddress },
            )

            PasswordField(
                value = state.password,
                onValueChange = viewModel::onPasswordChanged,
                onDone = viewModel::onSubmit,
                enabled = !state.submitting,
                isNewPassword = true,
                errorText = if (state.error == CreateAccountError.WeakPassword) {
                    stringResource(Res.string.signin_email_error_weak_password)
                } else null,
                modifier = Modifier.fillMaxWidth(),
            )

            PasswordRules(metRules = state.metRules)

            if (state.error == CreateAccountError.Failed) {
                Text(
                    text = stringResource(Res.string.signin_error_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = viewModel::onSubmit,
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
                        PasswordRule.MinLength -> stringResource(Res.string.create_account_password_rule_length, PASSWORD_MIN_LENGTH)
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
