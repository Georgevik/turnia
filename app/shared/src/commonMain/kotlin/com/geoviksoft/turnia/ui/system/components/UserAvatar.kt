package com.geoviksoft.turnia.ui.system.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import com.geoviksoft.turnia.ui.system.color.ColorUtils
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.animal_icon_bat

@Composable
fun UserAvatar(
    modifier: Modifier = Modifier,
    background: Color,
    animalIcon: DrawableResource
) {
    val tintColor = remember { ColorUtils.getContrastingColor(background) }

    Box(
        modifier = modifier.background(background, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(animalIcon),
            contentDescription = null,
            tint = tintColor,
        )
    }
}


@Preview
@Composable
fun UserAvatarPreview() {
    UserAvatar(
        modifier = Modifier.size(64.dp),
        background = Color.Yellow,
        animalIcon = Res.drawable.animal_icon_bat
    )
}
