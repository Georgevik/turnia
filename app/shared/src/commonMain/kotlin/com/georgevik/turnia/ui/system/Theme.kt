package com.georgevik.turnia.ui.system

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Teal500,
    onPrimary = White,
    primaryContainer = Teal300,
    onPrimaryContainer = Teal800,
    inversePrimary = Teal200,
    secondary = Slate500,
    onSecondary = White,
    secondaryContainer = Slate200,
    onSecondaryContainer = Slate600,
    tertiary = Amber500,
    onTertiary = White,
    tertiaryContainer = Amber300,
    onTertiaryContainer = Amber800,
    error = Red500,
    onError = White,
    errorContainer = Red100,
    onErrorContainer = Red800,
    background = Grey50,
    onBackground = Grey900,
    surface = Grey50,
    onSurface = Grey900,
    surfaceVariant = Grey300,
    onSurfaceVariant = Grey700,
    surfaceTint = Teal500,
    inverseSurface = Grey800,
    inverseOnSurface = Grey150,
    surfaceDim = Grey400,
    surfaceBright = Grey50,
    surfaceContainerLowest = White,
    surfaceContainerLow = Grey100,
    surfaceContainer = Grey200,
    surfaceContainerHigh = Grey250,
    surfaceContainerHighest = Grey300,
    outline = Grey600,
    outlineVariant = Grey450,
)

private val DarkColorScheme = darkColorScheme(
    primary = Teal200,
    onPrimary = Teal800,
    primaryContainer = Teal500,
    onPrimaryContainer = Teal200,
    secondary = Slate200,
    onSecondary = Black,
    tertiary = Amber300,
    onTertiary = Amber800,
    background = Black,
    onBackground = Grey50,
    surface = Grey800,
    onSurface = Grey50,
    surfaceVariant = Grey700,
    onSurfaceVariant = Grey450,
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
