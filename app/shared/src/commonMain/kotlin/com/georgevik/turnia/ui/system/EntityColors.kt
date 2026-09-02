package com.georgevik.turnia.ui.system

import androidx.compose.ui.graphics.Color
import com.georgevik.turnia.core.system.ALL_COLORS

/**
 * A fixed, hand-picked palette used to color group/colleague accents. These are
 * accents, so every entry is a vivid, saturated tone (HSV saturation >= 0.58) —
 * no muted/pastel colors — and dark enough to carry a white icon or white text.
 * Hues are spread around the wheel so adjacent ids look clearly different.
 */
val EntityPalette = ALL_COLORS.mapNotNull { it.toComposeColorOrNull() }

/**
 * Deterministic accent color for an entity from its [id] — the same id always
 * maps to the same color, on every platform (Kotlin's String.hashCode is stable),
 * so a group's colored bar (or a colleague's avatar) is consistent everywhere.
 */
fun entityColor(id: String): Color {
    val index = (id.hashCode() and Int.MAX_VALUE) % EntityPalette.size
    return EntityPalette[index]
}
