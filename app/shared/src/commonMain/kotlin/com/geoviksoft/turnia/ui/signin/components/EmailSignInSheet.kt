package com.geoviksoft.turnia.ui.signin.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.geoviksoft.turnia.ui.signin.model.EmailForm
import com.geoviksoft.turnia.ui.signin.model.EmailFormError
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.profile_field_email
import turnia.app.shared.generated.resources.signin_email_create_account
import turnia.app.shared.generated.resources.signin_email_error_invalid_credentials
import turnia.app.shared.generated.resources.signin_email_error_invalid_email
import turnia.app.shared.generated.resources.signin_email_error_password_required
import turnia.app.shared.generated.resources.signin_email_forgot_password
import turnia.app.shared.generated.resources.signin_email_no_account
import turnia.app.shared.generated.resources.signin_email_reset_sent
import turnia.app.shared.generated.resources.signin_email_sign_in
import turnia.app.shared.generated.resources.signin_email_title
import turnia.app.shared.generated.resources.signin_error_failed

@Composable
fun EmailSignInSheet(
    form: EmailForm,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onForgotPassword: () -> Unit,
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(Res.string.signin_email_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        OutlinedTextField(
            value = form.email,
            onValueChange = onEmailChange,
            label = { Text(stringResource(Res.string.profile_field_email)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            singleLine = true,
            enabled = !form.submitting,
            isError = form.error == EmailFormError.InvalidEmail,
            supportingText = form.error
                ?.takeIf { it == EmailFormError.InvalidEmail }
                ?.let { { Text(stringResource(it.message())) } },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.EmailAddress },
        )

        PasswordField(
            value = form.password,
            onValueChange = onPasswordChange,
            onDone = onSubmit,
            enabled = !form.submitting,
            isNewPassword = false,
            errorText = form.error
                ?.takeIf { it == EmailFormError.PasswordRequired }
                ?.let { stringResource(it.message()) },
            modifier = Modifier.fillMaxWidth(),
        )

        form.error?.takeIf { it == EmailFormError.InvalidCredentials || it == EmailFormError.Failed }?.let {
            Text(
                text = stringResource(it.message()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        form.resetSentTo?.let { email ->
            Text(
                text = stringResource(Res.string.signin_email_reset_sent, email),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Button(
            onClick = onSubmit,
            enabled = !form.submitting,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            if (form.submitting) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(Res.string.signin_email_sign_in))
            }
        }

        TextButton(
            onClick = onForgotPassword,
            enabled = !form.submitting,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(stringResource(Res.string.signin_email_forgot_password))
        }

        HorizontalDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.signin_email_no_account),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onCreateAccount, enabled = !form.submitting) {
                Text(stringResource(Res.string.signin_email_create_account))
            }
        }
    }
}

private fun EmailFormError.message(): StringResource = when (this) {
    EmailFormError.InvalidEmail -> Res.string.signin_email_error_invalid_email
    EmailFormError.PasswordRequired -> Res.string.signin_email_error_password_required
    EmailFormError.InvalidCredentials -> Res.string.signin_email_error_invalid_credentials
    EmailFormError.Failed -> Res.string.signin_error_failed
}
