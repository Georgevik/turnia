package com.geoviksoft.turnia.ui.system

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun TurniaSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        val isError = (data.visuals as? TurniaSnackbarVisual)?.isError ?: false

        Snackbar(
            snackbarData = data,
            containerColor = if (isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                SnackbarDefaults.color
            },
            contentColor = if (isError) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                SnackbarDefaults.contentColor
            },
        )
    }
}
