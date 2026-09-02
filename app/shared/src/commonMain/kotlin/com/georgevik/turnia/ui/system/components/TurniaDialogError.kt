package com.georgevik.turnia.ui.system.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.dialog_error_close
import turnia.app.shared.generated.resources.dialog_error_retry
import turnia.app.shared.generated.resources.dialog_error_title

/**
 * Blocking error dialog for failures that leave a screen with nothing to render. Transient failures
 * that still leave the screen usable belong in a snackbar instead — see *Error handling & UI state*
 * in CLAUDE.md.
 */
@Composable
fun TurniaDialogError(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(Res.string.dialog_error_title),
    dismissText: String = stringResource(Res.string.dialog_error_close),
    onRetry: (() -> Unit)? = null,
    retryText: String = stringResource(Res.string.dialog_error_retry),
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        icon = { Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null) },
        iconContentColor = MaterialTheme.colorScheme.error,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onRetry ?: onDismiss) {
                Text(if (onRetry != null) retryText else dismissText)
            }
        },
        dismissButton = if (onRetry != null) {
            { TextButton(onClick = onDismiss) { Text(dismissText) } }
        } else {
            null
        },
    )
}
