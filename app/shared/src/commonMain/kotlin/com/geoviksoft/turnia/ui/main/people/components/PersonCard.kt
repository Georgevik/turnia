package com.geoviksoft.turnia.ui.main.people.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.domain.model.UserProfile
import com.geoviksoft.turnia.ui.main.people.model.PersonRowUi
import com.geoviksoft.turnia.ui.system.PreviewTurniaTheme
import com.geoviksoft.turnia.ui.system.components.TListItem
import com.geoviksoft.turnia.ui.system.components.UserAvatar
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.share_calendar_unknown_user

@Composable
fun PersonCard(
    person: PersonRowUi,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    TListItem(
        title = person.displayName(),
        modifier = modifier,
        subtitle = person.username.takeIf { it.isNotBlank() }?.let { "@$it" },
        onClick = onClick,
        onLongClick = onLongClick,
        leading = { UserAvatar(avatar = person.avatar) },
        trailing = trailing,
    )
}

/** A user whose profile could not be read has no name to show, so the row names the gap. */
@Composable
fun PersonRowUi.displayName(): String =
    name.ifBlank { stringResource(Res.string.share_calendar_unknown_user) }

@Preview
@Composable
fun PersonCardPreview() {
    PreviewTurniaTheme {
        Box(Modifier.background(Color.White)) {
            PersonCard(
                person = PersonRowUi(
                    id = UserId("abc"),
                    name = "MyName",
                    username = "username",
                    avatar = UserProfile.AnimalAvatar.PREVIEW
                )
            )
        }
    }
}
