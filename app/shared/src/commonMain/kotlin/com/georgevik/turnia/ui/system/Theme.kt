package com.georgevik.turnia.ui.system

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Teal500,
    onPrimary = White,
    primaryContainer = Teal100,
    onPrimaryContainer = Teal900,
    secondary = Slate500,
    onSecondary = White,
    tertiary = Amber500,
    onTertiary = Amber900,
    background = Cream200,
    onBackground = Black,
    surface = Cream100,
    onSurface = Black,
    surfaceVariant = Cream300,
    onSurfaceVariant = Cream800,
)

private val DarkColorScheme = darkColorScheme(
    primary = Teal300,
    onPrimary = Teal900,
    primaryContainer = Teal700,
    onPrimaryContainer = Teal100,
    secondary = Slate200,
    onSecondary = Black,
    tertiary = Amber300,
    onTertiary = Amber900,
    background = Black,
    onBackground = Cream200,
    surface = Grey800,
    onSurface = Cream200,
    surfaceVariant = Grey700,
    onSurfaceVariant = Grey300,
)

/**
 * Applies the Turnia brand colors (see Color.kt) to Material 3. Follows the
 * system light/dark setting by default; pass [darkTheme] to force one.
 *
 * Note: Android "Material You" dynamic color is intentionally not used — it
 * relies on Android-only APIs (`Build`, `LocalContext`) that are unavailable in
 * `commonMain`. Wiring it in would require an expect/actual bridge.
 */
@Composable
fun TurniaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content,
    )
}
