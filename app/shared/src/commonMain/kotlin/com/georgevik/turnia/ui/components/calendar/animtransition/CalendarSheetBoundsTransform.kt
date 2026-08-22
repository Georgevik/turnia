package com.georgevik.turnia.ui.components.calendar.animtransition

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Rect

internal const val CALENDAR_TRANSITION_MILLIS = 300

/**
 * Bounds/position spring shared by the tile→sheet container transform (surface and
 * day number). A gentle, slightly-damped spring gives the tile a natural "grow into
 * the sheet" feel without overshoot.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
internal val CalendarSheetBoundsTransform =
    BoundsTransform { _: Rect, _: Rect ->
        tween(CALENDAR_TRANSITION_MILLIS, easing = FastOutSlowInEasing)
    }
