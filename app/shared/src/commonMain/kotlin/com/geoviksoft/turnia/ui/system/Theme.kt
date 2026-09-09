package com.geoviksoft.turnia.ui.system

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.geoviksoft.turnia.navigation.LocalNavigator
import com.geoviksoft.turnia.navigation.LocalRootNavigator
import com.geoviksoft.turnia.navigation.Navigator

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
    primaryContainer = Teal700,
    onPrimaryContainer = Teal200,
    inversePrimary = Teal500,
    secondary = Slate200,
    onSecondary = Slate700,
    secondaryContainer = Slate700,
    onSecondaryContainer = Slate200,
    tertiary = Amber300,
    onTertiary = Amber800,
    tertiaryContainer = Amber700,
    onTertiaryContainer = Amber300,
    error = Red200,
    onError = Red900,
    errorContainer = Red800,
    onErrorContainer = Red100,
    background = Ink900,
    onBackground = Mist100,
    surface = Ink800,
    onSurface = Mist100,
    surfaceVariant = Ink650,
    onSurfaceVariant = Mist300,
    surfaceTint = Teal200,
    inverseSurface = Mist100,
    inverseOnSurface = Ink750,
    surfaceDim = Ink900,
    surfaceBright = Ink600,
    surfaceContainerLowest = Ink850,
    surfaceContainerLow = Ink800,
    surfaceContainer = Ink750,
    surfaceContainerHigh = Ink700,
    surfaceContainerHighest = Ink650,
    outline = Mist500,
    outlineVariant = Mist700,
)

val LocalPaddings = staticCompositionLocalOf { Padding(S = 45.dp) }

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

@Composable
fun PreviewTurniaTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalRootNavigator provides FakeNavigator,
        LocalNavigator provides FakeNavigator,
        LocalSnackbar provides SnackbarHostState(),
    ) {
        TurniaTheme(content = content)
    }
}

private val FakeNavigator = object : Navigator {
    override fun goTo(route: NavKey) = Unit

    override fun goBack() = Unit

    override fun popToRoot() = Unit
}
