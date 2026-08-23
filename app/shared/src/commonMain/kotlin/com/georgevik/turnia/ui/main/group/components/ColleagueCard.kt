package com.georgevik.turnia.ui.main.group.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.georgevik.turnia.ui.main.group.model.ColleageRowUi
import com.georgevik.turnia.ui.system.entityColor

@Composable
fun ColleagueCard(colleague: ColleageRowUi, onClick: () -> Unit) {
    val avatarColor = entityColor(colleague.id)
    EntityCard(onClick = onClick) {
        Avatar(background = avatarColor, icon = Icons.Default.Person)
        GroupCalendarRow(
            name = colleague.name,
            modifier = Modifier.weight(1f),
        )
        Chevron()
    }
}
