package com.geoviksoft.turnia.ui.system

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals

class TurniaSnackbarVisual(
    override val message: String,
    val isError: Boolean = false,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration = SnackbarDuration.Short
) : SnackbarVisuals

fun String.toErrorSnackbar() = TurniaSnackbarVisual(this, isError = true)
