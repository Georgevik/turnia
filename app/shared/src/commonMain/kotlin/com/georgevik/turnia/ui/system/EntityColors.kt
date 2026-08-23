package com.georgevik.turnia.ui.system

import androidx.compose.ui.graphics.Color

/**
 * A fixed, hand-picked palette used to color group/colleague accents. These are
 * accents, so every entry is a vivid, saturated tone (HSV saturation >= 0.58) —
 * no muted/pastel colors — and dark enough to carry a white icon or white text.
 * Hues are spread around the wheel so adjacent ids look clearly different.
 */
private val EntityPalette = listOf(
    Color(0xFFE53935), // red
    Color(0xFFF4511E), // deep orange
    Color(0xFFFB8C00), // orange
    Color(0xFFF9A825), // gold
    Color(0xFF7CB342), // lime
    Color(0xFF43A047), // green
    Color(0xFF00897B), // teal
    Color(0xFF00ACC1), // cyan
    Color(0xFF039BE5), // light blue
    Color(0xFF1E88E5), // blue
    Color(0xFF3949AB), // indigo
    Color(0xFF5E35B1), // deep purple
    Color(0xFF8E24AA), // violet
    Color(0xFFD81B60), // pink
)

/**
 * Deterministic accent color for an entity from its [id] — the same id always
 * maps to the same color, on every platform (Kotlin's String.hashCode is stable),
 * so a group's colored bar (or a colleague's avatar) is consistent everywhere.
 */
fun entityColor(id: String): Color {
    val index = (id.hashCode() and Int.MAX_VALUE) % EntityPalette.size
    return EntityPalette[index]
}
