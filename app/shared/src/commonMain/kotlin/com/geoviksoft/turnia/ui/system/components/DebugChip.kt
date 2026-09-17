package com.geoviksoft.turnia.ui.system.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.geoviksoft.turnia.ui.system.color.DebugOrange
import com.geoviksoft.turnia.ui.system.color.White
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.debug_chip

@Composable
fun DebugChip(modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = CircleShape, color = DebugOrange, contentColor = White) {
        Text(
            text = stringResource(Res.string.debug_chip),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }
}

@Preview
@Composable
fun DebugChipPreview() = DebugChip()
