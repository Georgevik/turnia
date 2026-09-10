package com.geoviksoft.turnia.ui.system.color

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

/**
 * Black or white, whichever can be read on top of this colour.
 *
 * Event backgrounds are chosen by users, so nothing about them is known at build time and every
 * surface painted with one has to pick its own text colour.
 */
fun Color.readableTextColor(): Color =
    if (luminance() > READABLE_ON_LIGHT) Color.Black else Color.White

private const val READABLE_ON_LIGHT = 0.5f

object ColorUtils {
    fun getContrastingColor(backgroundColor: Color): Color {
        val (hue, _, _) = backgroundColor.toHsl()
        val isDark = backgroundColor.luminance() < 0.45f

        return Color.hsl(
            hue = hue,
            saturation = 0.25f, // Keep saturation low for a pleasant, muted tone
            lightness = if (isDark) 0.90f else 0.15f // Very bright pastel vs. deep dark tone
        )
    }

    // Pure Kotlin RGB to HSL conversion
    private fun Color.toHsl(): FloatArray {
        val r = red
        val g = green
        val b = blue

        val max = max(r, max(g, b))
        val min = min(r, min(g, b))
        val delta = max - min

        var h = 0f
        val l = (max + min) / 2f
        val s = if (delta == 0f) 0f else delta / (1f - kotlin.math.abs(2f * l - 1f))

        if (delta != 0f) {
            h = when (max) {
                r -> ((g - b) / delta) % 6f
                g -> ((b - r) / delta) + 2f
                else -> ((r - g) / delta) + 4f
            } * 60f
            if (h < 0f) h += 360f
        }

        return floatArrayOf(h, s, l)
    }
}
