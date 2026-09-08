package com.georgevik.turnia.ui.main.people.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.ui.main.people.model.PersonRowUi
import com.georgevik.turnia.ui.system.components.Avatar
import com.georgevik.turnia.ui.system.components.TListItem
import com.georgevik.turnia.ui.system.entityColor
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.share_calendar_unknown_user

@Composable
fun PersonCard(
    person: PersonRowUi,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    TListItem(
        title = person.displayName(),
        subtitle = person.username.takeIf { it.isNotBlank() }?.let { "@$it" },
        onClick = onClick,
        leading = { PersonAvatar(person.id) },
        trailing = trailing,
    )
}

@Composable
fun PersonAvatar(id: UserId) =
    Avatar(background = entityColor(id.value), icon = Icons.Default.Person)

/** A user with no username reservation has no name to resolve either, so the row names the gap. */
@Composable
fun PersonRowUi.displayName(): String =
    name.ifBlank { stringResource(Res.string.share_calendar_unknown_user) }
