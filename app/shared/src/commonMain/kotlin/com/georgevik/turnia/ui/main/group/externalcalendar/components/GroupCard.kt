package com.georgevik.turnia.ui.main.group.externalcalendar.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.runtime.Composable
import com.georgevik.turnia.ui.main.group.calendarlist.model.GroupRowUi
import com.georgevik.turnia.ui.system.components.Avatar
import com.georgevik.turnia.ui.system.components.Chevron
import com.georgevik.turnia.ui.system.components.TListItem
import com.georgevik.turnia.ui.system.entityColor
import org.jetbrains.compose.resources.pluralStringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.group_member_count

@Composable
fun GroupCard(group: GroupRowUi, onClick: () -> Unit) {
    TListItem(
        title = group.name,
        subtitle = pluralStringResource(
            Res.plurals.group_member_count,
            group.members,
            group.members,
        ),
        onClick = onClick,
        leading = { Avatar(background = entityColor(group.id), icon = Icons.Default.Groups) },
        trailing = { Chevron() },
    )
}
