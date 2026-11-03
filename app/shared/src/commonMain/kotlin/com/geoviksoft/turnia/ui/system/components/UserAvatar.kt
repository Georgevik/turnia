package com.geoviksoft.turnia.ui.system.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import coil3.compose.AsyncImage
import com.geoviksoft.turnia.core.domain.model.ProfilePhoto
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.ui.system.TestTags
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
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

/**
 * A person's [photo] when they have one, over their animal on its colour. The animal is always drawn
 * first, so a photo still loading, failing or out of reach offline leaves the animal showing rather
 * than an empty circle.
 */
@Composable
fun UserAvatar(
    avatar: UserProfile.AnimalAvatar,
    size: UserAvatarSize = UserAvatarSize.M,
    modifier: Modifier = Modifier,
    photo: ProfilePhoto? = null,
) {
    Box(modifier.size(size.value)) {
        UserAvatar(
            modifier = Modifier.fillMaxSize(),
            background = avatar.background?.toComposeColorOrNull() ?: EntityPalette.first(),
            animalIcon = animalIcon(avatar.animal ?: "bat"),
        )
        if (photo != null) {
            // Tagged only once loaded: what a test can see is the photo actually drawn.
            var loaded by remember(photo) { mutableStateOf(false) }
            AsyncImage(
                model = photo,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onSuccess = { loaded = true },
                modifier = Modifier.fillMaxSize().clip(CircleShape)
                    .then(if (loaded) Modifier.testTag(TestTags.avatarPhoto(photo.userId)) else Modifier),
            )
        }
    }
}

enum class UserAvatarSize(val value : Dp) {
    S(32.dp),
    M(48.dp),
    L(64.dp)
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
                    background = EntityPalette[2].toHex()
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

/** With a photo that has not loaded — in a preview it never does — the animal is what shows. */
@Preview
@Composable
fun UserAvatarPhotoLoadingPreview() {
    PreviewTurniaTheme {
        Box {
            UserAvatar(
                avatar = UserProfile.AnimalAvatar.PREVIEW,
                size = UserAvatarSize.L,
                photo = ProfilePhoto(UserId("bruno"), "p1"),
            )
        }
    }
}
