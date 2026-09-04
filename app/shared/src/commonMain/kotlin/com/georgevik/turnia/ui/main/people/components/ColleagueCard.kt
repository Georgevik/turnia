package com.georgevik.turnia.ui.main.people.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import com.georgevik.turnia.ui.main.people.model.ColleagueRowUi
import com.georgevik.turnia.ui.system.components.Avatar
import com.georgevik.turnia.ui.system.components.Chevron
import com.georgevik.turnia.ui.system.components.TListItem
import com.georgevik.turnia.ui.system.entityColor

@Composable
fun ColleagueCard(colleague: ColleagueRowUi, onClick: () -> Unit) {
    TListItem(
        title = colleague.name,
        subtitle = colleague.username.takeIf { it.isNotBlank() }?.let { "@$it" },
        onClick = onClick,
        leading = { Avatar(background = entityColor(colleague.id.value), icon = Icons.Default.Person) },
        trailing = { Chevron() },
    )
}
