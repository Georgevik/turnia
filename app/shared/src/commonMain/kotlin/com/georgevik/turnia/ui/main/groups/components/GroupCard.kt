package com.georgevik.turnia.ui.main.groups.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.georgevik.turnia.core.domain.model.GroupId
import com.georgevik.turnia.ui.main.groups.model.GroupRowUi
import com.georgevik.turnia.ui.system.TurniaTheme
import com.georgevik.turnia.ui.system.components.AdminBadge
import com.georgevik.turnia.ui.system.components.Avatar
import com.georgevik.turnia.ui.system.components.Chevron
import com.georgevik.turnia.ui.system.components.RevokedBadge
import com.georgevik.turnia.ui.system.components.TListItem
import com.georgevik.turnia.ui.system.entityColor
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.group_member_count
import turnia.app.shared.generated.resources.group_revoked_banner

@Composable
fun GroupCard(group: GroupRowUi, showAdminBadge: Boolean = true, onClick: () -> Unit) {
    TListItem(
        title = group.name,
        // A group they were removed from has no roster to count: what is left is the reason it
        // is still on the list.
        subtitle = if (group.isRevoked) {
            stringResource(Res.string.group_revoked_banner)
        } else {
            pluralStringResource(Res.plurals.group_member_count, group.members, group.members)
        },
        onClick = onClick,
        leading = { Avatar(background = entityColor(group.id.value), icon = Icons.Default.Groups) },
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (group.isRevoked) RevokedBadge()
                else if (group.isAdmin && showAdminBadge) AdminBadge()
                Chevron()
            }
        },
    )
}

@Preview
@Composable
private fun GroupCardPreview() {
    TurniaTheme {
        GroupCard(
            group = GroupRowUi(
                id = GroupId("1"),
                name = "Group Name",
                members = 5,
                isAdmin = true,
            ),
            onClick = {},
        )
    }
}

