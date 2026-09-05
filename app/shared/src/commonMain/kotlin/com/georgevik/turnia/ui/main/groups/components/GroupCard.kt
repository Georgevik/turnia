package com.georgevik.turnia.ui.main.groups.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.georgevik.turnia.ui.main.groups.model.GroupRowUi
import com.georgevik.turnia.ui.system.components.AdminBadge
import com.georgevik.turnia.ui.system.components.Avatar
import com.georgevik.turnia.ui.system.components.Chevron
import com.georgevik.turnia.ui.system.components.TListItem
import com.georgevik.turnia.ui.system.entityColor
import org.jetbrains.compose.resources.pluralStringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.group_member_count

@Composable
fun GroupCard(group: GroupRowUi, showAdminBadge: Boolean = true, onClick: () -> Unit) {
    TListItem(
        title = group.name,
        subtitle = pluralStringResource(
            Res.plurals.group_member_count,
            group.members,
            group.members,
        ),
        onClick = onClick,
        leading = { Avatar(background = entityColor(group.id.value), icon = Icons.Default.Groups) },
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (group.isAdmin && showAdminBadge) AdminBadge()
                Chevron()
            }
        },
    )
}
