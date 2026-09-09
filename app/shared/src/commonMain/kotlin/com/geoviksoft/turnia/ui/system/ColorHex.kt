package com.geoviksoft.turnia.ui.system

import androidx.compose.ui.graphics.Color

/**
 * Bridges the `#RRGGBB` / `#AARRGGBB` hex strings used in the data layer (matching
 * the Firestore `groupEventTypeColors` / `personalEventType.color` format) to Compose
 * [Color] and back. Group/personal types store colors as strings; the UI draws [Color].
 */

/** Parses a `#RRGGBB` or `#AARRGGBB` hex string, or returns [fallback] if malformed. */
fun String.toComposeColorOr(fallback: Color): Color = toComposeColorOrNull() ?: fallback

/** Parses a `#RRGGBB` or `#AARRGGBB` hex string, or `null` if malformed. */
fun String.toComposeColorOrNull(): Color? {
    val hex = trim().removePrefix("#")
    val argb = when (hex.length) {
        6 -> hex.toLongOrNull(16)?.let { 0xFF000000 or it }
        8 -> hex.toLongOrNull(16)
        else -> null
    } ?: return null
    return Color(argb)
}

/** Serializes to an opaque `#RRGGBB` hex string (alpha dropped). */
fun Color.toHex(): String {
    fun channel(v: Float): String {
        val i = (v * 255f).toInt().coerceIn(0, 255)
        return i.toString(16).uppercase().padStart(2, '0')
    }
    return "#${channel(red)}${channel(green)}${channel(blue)}"
}
