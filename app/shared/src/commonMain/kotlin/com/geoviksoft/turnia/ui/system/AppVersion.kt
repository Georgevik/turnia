package com.geoviksoft.turnia.ui.system

import androidx.compose.runtime.Composable

/** The version the stores show (`1.0`) and the build number behind it (`1`). */
data class AppVersion(val name: String, val build: String)

@Composable
expect fun rememberAppVersion(): AppVersion
