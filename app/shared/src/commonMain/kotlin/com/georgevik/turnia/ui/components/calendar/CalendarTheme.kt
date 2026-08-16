package com.georgevik.turnia.ui.components.calendar

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

/**
 * Visual accents a [CalendarViewer] uses to mark days.
 *
 * @param accentColor border of the selected day and background fill of today.
 * @param selectedBackground background fill of the selected day.
 */
@Immutable
data class CalendarTheme(
    val accentColor: Color,
    val selectedBackground: Color,
)

/** Ready-made calendar themes derived from the app's Material color scheme. */
object CalendarThemes {

    @Composable
    fun primary(): CalendarTheme = CalendarTheme(
        accentColor = MaterialTheme.colorScheme.primary,
        selectedBackground = MaterialTheme.colorScheme.primary
            .copy(alpha = 0.15f)
            .compositeOver(MaterialTheme.colorScheme.surface),
    )

    @Composable
    fun tertiary(): CalendarTheme = CalendarTheme(
        accentColor = MaterialTheme.colorScheme.tertiary,
        selectedBackground = MaterialTheme.colorScheme.tertiary
            .copy(alpha = 0.15f)
            .compositeOver(MaterialTheme.colorScheme.surface),
    )
}
