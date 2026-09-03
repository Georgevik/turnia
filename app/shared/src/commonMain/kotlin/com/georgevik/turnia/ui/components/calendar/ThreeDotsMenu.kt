package com.georgevik.turnia.ui.components.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.georgevik.turnia.ui.components.calendar.model.ThreeDotsOption

@Composable
fun ThreeDotsContextMenu(options: List<ThreeDotsOption>) {
    if (options.isEmpty()) {
        return
    }

    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.wrapContentSize(Alignment.TopEnd)
    ) {
        // 3. Three dots trigger button
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options"
            )
        }

        // 4. Contextual dropdown menu
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.text) },
                    leadingIcon = {
                        option.leadingIcon?.let { Icon(imageVector = it, contentDescription = null) }
                    },
                    onClick = {
                        expanded = false
                        option.onClick()
                    }
                )
            }
        }
    }
}
