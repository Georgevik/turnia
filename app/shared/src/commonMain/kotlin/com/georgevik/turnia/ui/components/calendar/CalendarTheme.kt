package com.georgevik.turnia.ui.components.calendar

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

/** How strongly a non-primary calendar tints its page background. Tune here. */
private const val BACKGROUND_TINT_ALPHA = 0.3f

/**
 * Visual identity a [CalendarViewer] uses: the accent that marks days and the
 * page background, so each calendar (mine / a group / a colleague) reads as a
 * distinct place. Day tiles keep the app's common surface so they pop against
 * the tinted background.
 *
 * @param accentColor border of the selected day and background fill of today.
 * @param selectedBackground background fill of the selected day.
 * @param background the whole calendar page background.
 */
@Immutable
data class CalendarTheme(
    val accentColor: Color,
    val selectedBackground: Color,
    val background: Color,
)

/** Ready-made calendar themes derived from the app's Material color scheme. */
object CalendarThemes {

    /** The user's own calendar — primary accent over the app's usual background. */
    @Composable
    fun myCalendar(): CalendarTheme = themeFor(
        accent = MaterialTheme.colorScheme.primary,
        background = MaterialTheme.colorScheme.background,
    )

    /** A group calendar — tertiary accent over a tertiary-tinted background. */
    @Composable
    fun group(): CalendarTheme = themeFor(
        accent = MaterialTheme.colorScheme.tertiary,
        background = tintedBackground(MaterialTheme.colorScheme.tertiary),
    )

    /** Another colleague's calendar — secondary accent over a secondary-tinted background. */
    @Composable
    fun colleague(): CalendarTheme = themeFor(
        accent = MaterialTheme.colorScheme.secondary,
        background = tintedBackground(MaterialTheme.colorScheme.secondary),
    )

    @Composable
    private fun themeFor(accent: Color, background: Color): CalendarTheme = CalendarTheme(
        accentColor = accent,
        selectedBackground = accent
            .copy(alpha = 0.15f)
            .compositeOver(MaterialTheme.colorScheme.surface),
        background = background,
    )

    /** A colored wash over the base background to tint a non-primary calendar. */
    @Composable
    private fun tintedBackground(accent: Color): Color = accent
        .copy(alpha = BACKGROUND_TINT_ALPHA)
        .compositeOver(MaterialTheme.colorScheme.background)
}
