package com.georgevik.turnia

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.georgevik.turnia.navigation.TurniaNavDisplay
import com.georgevik.turnia.ui.system.TurniaSnackbarVisual
import com.georgevik.turnia.ui.system.TurniaTheme

@Composable
@Preview
fun App() {
    TurniaTheme {
        val snackbarHostState = remember { SnackbarHostState() }

        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
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
                        }
                    )
                }
            }
        ) { innerPadding ->
            TurniaNavDisplay(snackbarHostState)
        }
    }
}
