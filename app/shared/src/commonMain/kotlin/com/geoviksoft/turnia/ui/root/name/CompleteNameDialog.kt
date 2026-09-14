package com.geoviksoft.turnia.ui.root.name

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.complete_name_continue
import turnia.app.shared.generated.resources.complete_name_message
import turnia.app.shared.generated.resources.complete_name_title
import turnia.app.shared.generated.resources.dialog_cancel
import turnia.app.shared.generated.resources.profile_error_name_required
import turnia.app.shared.generated.resources.profile_error_save
import turnia.app.shared.generated.resources.profile_field_name

/** Only its buttons close it: tapping outside or going back would let the user in without a name. */
@Composable
fun CompleteNameDialog(viewModel: CompleteNameViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(stringResource(Res.string.complete_name_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(Res.string.complete_name_message))
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChanged,
                    label = { Text(stringResource(Res.string.profile_field_name)) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                    singleLine = true,
                    enabled = !state.saving,
                    isError = state.error != null,
                    supportingText = state.error?.let { { Text(stringResource(it.message())) } },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = viewModel::onContinue, enabled = !state.saving && state.name.isNotBlank()) {
                if (state.saving) CircularProgressIndicator(Modifier.size(20.dp))
                else Text(stringResource(Res.string.complete_name_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::onCancel, enabled = !state.saving) {
                Text(stringResource(Res.string.dialog_cancel))
            }
        },
    )
}

private fun CompleteNameError.message() = when (this) {
    CompleteNameError.NameRequired -> Res.string.profile_error_name_required
    CompleteNameError.SaveFailed -> Res.string.profile_error_save
}
