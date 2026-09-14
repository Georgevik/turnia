package com.geoviksoft.turnia.ui.signin.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.signin_email_hide_password
import turnia.app.shared.generated.resources.signin_email_password
import turnia.app.shared.generated.resources.signin_email_show_password

@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    enabled: Boolean,
    isNewPassword: Boolean,
    modifier: Modifier = Modifier,
    errorText: String? = null,
) {
    var visible by rememberSaveable { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(Res.string.signin_email_password)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = stringResource(
                        if (visible) Res.string.signin_email_hide_password else Res.string.signin_email_show_password
                    ),
                )
            }
        },
        singleLine = true,
        enabled = enabled,
        isError = errorText != null,
        supportingText = errorText?.let { { Text(it) } },
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.semantics {
            contentType = if (isNewPassword) ContentType.NewPassword else ContentType.Password
        },
    )
}
