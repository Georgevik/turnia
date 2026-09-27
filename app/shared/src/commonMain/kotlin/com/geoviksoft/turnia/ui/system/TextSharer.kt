package com.geoviksoft.turnia.ui.system

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/** Hands a piece of text to the platform: onto its clipboard, or out through its share sheet. */
interface TextSharer {
    fun copy(text: String)
    fun share(text: String)
}

@Composable
expect fun rememberTextSharer(): TextSharer

/**
 * The sharer screens use, provided at the root. A local rather than [rememberTextSharer] at each call
 * site so a test can swap in one that records what would have been shared: the system's share sheet
 * is out of its reach.
 */
val LocalTextSharer: ProvidableCompositionLocal<TextSharer> = staticCompositionLocalOf {
    error("LocalTextSharer not provided — this content must be hosted under the app's root")
}
