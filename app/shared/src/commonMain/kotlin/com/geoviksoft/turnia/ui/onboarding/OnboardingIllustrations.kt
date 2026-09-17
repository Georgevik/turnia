package com.geoviksoft.turnia.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.geoviksoft.turnia.ui.system.color.EntityPalette
import com.geoviksoft.turnia.ui.system.components.TurniaLogo
import com.geoviksoft.turnia.ui.system.components.UserAvatar
import org.jetbrains.compose.resources.DrawableResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.animal_icon_fox
import turnia.app.shared.generated.resources.animal_icon_koala
import turnia.app.shared.generated.resources.animal_icon_penguin

internal val IllustrationHeight = 240.dp
private val MockupWidth = 272.dp

private data class Colleague(val animal: DrawableResource, val color: Color)

private val Fox = Colleague(Res.drawable.animal_icon_fox, EntityPalette[1 % EntityPalette.size])
private val Penguin = Colleague(Res.drawable.animal_icon_penguin, EntityPalette[4 % EntityPalette.size])
private val Koala = Colleague(Res.drawable.animal_icon_koala, EntityPalette[7 % EntityPalette.size])

/** A shift and its history: it went from one colleague to the next, twice. */
@Composable
internal fun SwapChainMockup() {
    IllustrationFrame {
        MockupCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(width = 6.dp, height = 36.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Placeholder(width = 110.dp, strong = true)
                    Placeholder(width = 72.dp)
                }
                TimePill("22:00")
            }

            Spacer(Modifier.height(16.dp))

            ChainStep(Fox, icon = Icons.Default.SwapHoriz, current = false)
            ChainConnector()
            ChainStep(Penguin, icon = Icons.Default.SwapHoriz, current = false)
            ChainConnector()
            ChainStep(Koala, icon = Icons.Default.Check, current = true)
        }
    }
}

/** A month where every shift already has someone on it. */
@Composable
internal fun CalendarMockup() {
    IllustrationFrame {
        MockupCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Placeholder(width = 96.dp, strong = true)
                Spacer(Modifier.weight(1f))
                UserAvatar(background = Koala.color, animalIcon = Koala.animal, modifier = Modifier.size(24.dp))
            }

            Spacer(Modifier.height(14.dp))

            val shifts = mapOf(
                2 to 0, 3 to 0, 6 to 1, 9 to 2, 10 to 2, 13 to 0,
                16 to 1, 17 to 1, 20 to 2, 23 to 0, 24 to 0, 27 to 1,
            )
            val today = 17
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(4) { week ->
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        repeat(7) { weekday ->
                            val day = week * 7 + weekday
                            val color = shifts[day]?.let { listOf(Fox, Penguin, Koala)[it].color }
                            DayCell(color = color, today = day == today, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Badge(
            icon = Icons.Default.Check,
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-8).dp, y = (-8).dp),
        )
    }
}

/** Two pushes landing, the newest on top. */
@Composable
internal fun NotificationMockup() {
    IllustrationFrame {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            NotificationCard(
                colleague = Penguin,
                modifier = Modifier.scale(0.9f).alpha(0.55f).offset(y = 20.dp),
            )
            NotificationCard(colleague = Fox)
        }

        Badge(
            icon = Icons.Default.NotificationsActive,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = (-12).dp, y = 8.dp),
        )
    }
}

@Composable
private fun IllustrationFrame(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier.height(IllustrationHeight).width(MockupWidth + 24.dp),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
private fun MockupCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.width(MockupWidth),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 6.dp,
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun ChainStep(colleague: Colleague, icon: ImageVector, current: Boolean) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (current) colors.primaryContainer else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(background = colleague.color, animalIcon = colleague.animal, modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(12.dp))
        Placeholder(width = if (current) 96.dp else 80.dp, strong = current, modifier = Modifier.weight(1f, fill = false))
        Spacer(Modifier.weight(1f))
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (current) colors.onPrimaryContainer else colors.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun ChainConnector() {
    Box(
        Modifier
            // Under the avatar's centre: the step's 8dp padding plus half of its 32dp.
            .padding(start = 23.dp)
            .size(width = 2.dp, height = 10.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@Composable
private fun DayCell(color: Color?, today: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(color ?: MaterialTheme.colorScheme.surfaceContainerHigh)
            .then(
                if (today) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier
            ),
    )
}

@Composable
private fun NotificationCard(colleague: Colleague, modifier: Modifier = Modifier) {
    MockupCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TurniaLogo(Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Placeholder(width = 120.dp, strong = true)
                Placeholder(width = 150.dp)
            }
            Spacer(Modifier.width(8.dp))
            UserAvatar(background = colleague.color, animalIcon = colleague.animal, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun TimePill(text: String) {
    // A clock time reads the same in every language the app speaks, so it can be real text.
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun Badge(icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** A line of text, without the text. */
@Composable
private fun Placeholder(width: Dp, modifier: Modifier = Modifier, strong: Boolean = false) {
    Box(
        modifier
            .width(width)
            .height(if (strong) 10.dp else 8.dp)
            .clip(CircleShape)
            .background(
                MaterialTheme.colorScheme.onSurface.copy(alpha = if (strong) 0.28f else 0.14f)
            )
    )
}
