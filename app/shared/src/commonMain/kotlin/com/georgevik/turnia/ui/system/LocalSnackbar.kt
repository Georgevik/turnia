package com.georgevik.turnia.ui.system

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

val LocalSnackbar: ProvidableCompositionLocal<SnackbarHostState> = staticCompositionLocalOf {
    error("LocalSnackbar not provided — this content must be hosted under MainScreen's Scaffold")
}
