package com.geoviksoft.turnia.ui.system.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.geoviksoft.turnia.core.domain.model.UserProfile
import com.geoviksoft.turnia.ui.system.PreviewTurniaTheme
import com.geoviksoft.turnia.ui.system.avatar.animalIcon
import com.geoviksoft.turnia.ui.system.color.ColorUtils
import com.geoviksoft.turnia.ui.system.color.EntityPalette
import com.geoviksoft.turnia.ui.system.color.toComposeColorOrNull
import com.geoviksoft.turnia.ui.system.color.toHex
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.animal_icon_duck

@Composable
fun UserAvatar(
    avatar: UserProfile.AnimalAvatar,
    modifier: Modifier = Modifier,
) {
    UserAvatar(
        modifier = modifier,
        background = avatar.background?.toComposeColorOrNull() ?: EntityPalette.first(),
        animalIcon = animalIcon(avatar.animal ?: "bat"),
    )
}

@Composable
fun UserAvatar(
    background: Color,
    animalIcon: DrawableResource?,
    modifier: Modifier = Modifier,
) {
    val tintColor = remember(background) { ColorUtils.getContrastingColor(background) }

    Box(
        modifier = modifier.background(background, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (animalIcon == null) {
            Icon(
                painter = painterResource(Res.drawable.animal_icon_duck),
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.fillMaxSize().padding(6.dp),
            )
        } else {
            Icon(
                painter = painterResource(animalIcon),
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.fillMaxSize().padding(6.dp),
            )
        }
    }
}

@Preview
@Composable
fun UserAvatarPreview() {
    PreviewTurniaTheme {
        Box {
            UserAvatar(
                avatar = UserProfile.AnimalAvatar(
                    animal = "duck",
                    background = EntityPalette.random().toHex()
                ),
                modifier = Modifier.size(64.dp),
            )
        }
    }
}

@Preview
@Composable
fun UserAvatarFallbackPreview() {
    PreviewTurniaTheme {
        Box {
            UserAvatar(
                avatar = UserProfile.AnimalAvatar.NONE,
                modifier = Modifier.size(64.dp),
            )
        }
    }
}
