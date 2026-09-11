package com.geoviksoft.turnia.ui.system

import androidx.compose.runtime.Composable

/** Hands a piece of text to the platform: onto its clipboard, or out through its share sheet. */
interface TextSharer {
    fun copy(text: String)
    fun share(text: String)
}

@Composable
expect fun rememberTextSharer(): TextSharer
