package com.geoviksoft.turnia.ui.components.event

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * The event type's acronym on its own colour, which is how a shift is recognised at a glance
 * everywhere it is listed.
 */
@Composable
fun AcronymChip(acronym: String, background: Color, textColor: Color) {
    val hasColor = background.isSpecified
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (hasColor) background else MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Text(
            text = acronym,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (hasColor) textColor else MaterialTheme.colorScheme.onSurface,
        )
    }
}
