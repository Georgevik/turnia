package com.georgevik.turnia.ui.main.groups.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.georgevik.turnia.ui.system.components.Avatar
import com.georgevik.turnia.ui.system.components.Chevron
import com.georgevik.turnia.ui.system.components.TListItem
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.groups_join_banner_subtitle
import turnia.app.shared.generated.resources.groups_join_title

@Composable
fun JoinGroupBanner(onClick: () -> Unit, modifier: Modifier = Modifier) {
    TListItem(
        title = stringResource(Res.string.groups_join_title),
        subtitle = stringResource(Res.string.groups_join_banner_subtitle),
        onClick = onClick,
        modifier = modifier,
        leading = {
            Avatar(
                background = MaterialTheme.colorScheme.primaryContainer,
                icon = Icons.Default.VpnKey,
            )
        },
        trailing = { Chevron() },
    )
}
