package com.georgevik.turnia.ui.main.group.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import com.georgevik.turnia.ui.main.group.model.ColleageRowUi
import com.georgevik.turnia.ui.system.components.Avatar
import com.georgevik.turnia.ui.system.components.Chevron
import com.georgevik.turnia.ui.system.components.TListItem
import com.georgevik.turnia.ui.system.entityColor

@Composable
fun ColleagueCard(colleague: ColleageRowUi, onClick: () -> Unit) {
    TListItem(
        title = colleague.name,
        subtitle = colleague.subtitle,
        onClick = onClick,
        leading = { Avatar(background = entityColor(colleague.id), icon = Icons.Default.Person) },
        trailing = { Chevron() },
    )
}
