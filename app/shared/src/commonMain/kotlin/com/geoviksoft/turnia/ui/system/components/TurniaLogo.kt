package com.geoviksoft.turnia.ui.system.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import com.geoviksoft.turnia.ui.system.LocalBuildInfo
import com.geoviksoft.turnia.ui.system.color.Camel
import com.geoviksoft.turnia.ui.system.color.DebugOrange
import com.geoviksoft.turnia.ui.system.color.White
import org.jetbrains.compose.resources.painterResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.logo

/**
 * The brand mark exactly as the launcher icon draws it — a black T on camel — in every theme.
 * Never tint it: following the colour scheme is what used to turn it green. The one exception is
 * the debug build, whose launcher icon is a white T on orange.
 */
@Composable
fun TurniaLogo(modifier: Modifier = Modifier) {
    val isDebug = LocalBuildInfo.current.isDebug
    Box(
        modifier = modifier.clip(CircleShape).background(if (isDebug) DebugOrange else Camel),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.logo),
            contentDescription = null,
            // The vector is drawn edge to edge, like a launcher foreground; the icon insets it the same.
            modifier = Modifier.fillMaxSize(0.6f),
            colorFilter = if (isDebug) ColorFilter.tint(White) else null,
        )
    }
}
