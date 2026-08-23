package com.georgevik.turnia.ui.main.group.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.georgevik.turnia.ui.main.group.model.GroupRowUi
import com.georgevik.turnia.ui.system.entityColor
import org.jetbrains.compose.resources.pluralStringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.group_member_count

@Composable
fun GroupCard(group: GroupRowUi, onClick: () -> Unit) {
    val barColor = entityColor(group.id)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Deterministic left color bar, keyed off the group id.
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(barColor),
            )
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Avatar(
                    background = MaterialTheme.colorScheme.surfaceContainerHigh,
                    icon = Icons.Default.Groups,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                )
                GroupCalendarRow(
                    name = group.name,
                    subtitle = pluralStringResource(
                        Res.plurals.group_member_count,
                        group.members,
                        group.members,
                    ),
                    modifier = Modifier.weight(1f),
                )
                Chevron()
            }
        }
    }
}
