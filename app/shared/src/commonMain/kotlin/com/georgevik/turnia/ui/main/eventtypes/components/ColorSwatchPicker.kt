package com.georgevik.turnia.ui.main.eventtypes.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.georgevik.turnia.ui.system.EntityPalette
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.event_type_color_selected

/**
 * Shows every [EntityPalette] color as a selectable circle. The currently selected
 * color is marked with a check. Selecting one calls [onPick].
 */
@Composable
fun ColorSwatchPicker(
    selected: Color?,
    onPick: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedLabel = stringResource(Res.string.event_type_color_selected)
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EntityPalette.forEach { color ->
            val isSelected = selected != null && color.value == selected.value
            Surface(
                shape = CircleShape,
                color = color,
                modifier = Modifier
                    .size(40.dp)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onPick(color) },
                    ),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = selectedLabel,
                            tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
            }
        }
    }
}
