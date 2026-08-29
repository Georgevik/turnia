package com.georgevik.turnia.navigation

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

val LocalNavigator: ProvidableCompositionLocal<Navigator> = staticCompositionLocalOf {
    error("LocalNavigator not provided — this content must be hosted under MainScreen's NavDisplay")
}
