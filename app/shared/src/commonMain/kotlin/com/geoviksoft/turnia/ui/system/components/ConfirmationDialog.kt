package com.geoviksoft.turnia.ui.system.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class ConfirmationStatus { Idle, Running, Succeeded, Failed }

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    confirmEnabled: Boolean = true,
    status: ConfirmationStatus = ConfirmationStatus.Idle,
) {
    val idle = status == ConfirmationStatus.Idle

    AlertDialog(
        onDismissRequest = { if (idle) onDismissRequest() },
        title = { Text(title) },
        text = { if (idle) Text(message) else StatusContent(status) },
        confirmButton = {
            if (idle) {
                TextButton(onClick = onConfirm, enabled = confirmEnabled) { Text(confirmText) }
            }
        },
        dismissButton = {
            if (idle) TextButton(onClick = onDismissRequest) { Text(dismissText) }
        },
    )
}

@Composable
private fun StatusContent(status: ConfirmationStatus) {
    Box(Modifier.fillMaxWidth().height(72.dp), Alignment.Center) {
        when (status) {
            ConfirmationStatus.Running -> CircularProgressIndicator()

            ConfirmationStatus.Succeeded -> Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            ConfirmationStatus.Failed -> Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error,
            )

            ConfirmationStatus.Idle -> Unit
        }
    }
}
