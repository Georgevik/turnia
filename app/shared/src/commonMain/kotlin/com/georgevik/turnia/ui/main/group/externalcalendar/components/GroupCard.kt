package com.georgevik.turnia.ui.main.group.externalcalendar.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.georgevik.turnia.ui.main.group.calendarlist.model.GroupRowUi
import com.georgevik.turnia.ui.system.components.Avatar
import com.georgevik.turnia.ui.system.components.Chevron
import com.georgevik.turnia.ui.system.components.TListItem
import com.georgevik.turnia.ui.system.entityColor
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.group_detail_open
import turnia.app.shared.generated.resources.group_member_count

@Composable
fun GroupCard(group: GroupRowUi, onClick: () -> Unit, onDetails: () -> Unit) {
    TListItem(
        title = group.name,
        subtitle = pluralStringResource(
            Res.plurals.group_member_count,
            group.members,
            group.members,
        ),
        onClick = onClick,
        leading = { Avatar(background = entityColor(group.id), icon = Icons.Default.Groups) },
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                IconButton(onClick = onDetails) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = stringResource(Res.string.group_detail_open),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Chevron()
            }
        },
    )
}
