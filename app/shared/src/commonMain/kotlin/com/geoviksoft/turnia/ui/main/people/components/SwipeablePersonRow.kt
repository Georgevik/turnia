package com.geoviksoft.turnia.ui.main.people.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.geoviksoft.turnia.ui.main.people.model.PersonRowUi
import com.geoviksoft.turnia.ui.system.TestTags

/** Which way a row is swiped to run its action, and so which side the action's icon shows on. */
enum class SwipeSide { END_TO_START, START_TO_END }

/**
 * A person row with one action reachable three ways: a swipe, a long press that opens a menu, and
 * an accessibility action — a swipe alone is invisible to TalkBack and hard to discover.
 */
@Composable
fun SwipeablePersonRow(
    person: PersonRowUi,
    side: SwipeSide,
    actionLabel: String,
    actionIcon: ImageVector,
    onAction: () -> Unit,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    resetKey: Any? = null,
) {
    val threshold = SwipeToDismissBoxDefaults.positionalThreshold
    val state = remember(resetKey) { SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, threshold) }
    val currentOnAction by rememberUpdatedState(onAction)
    val onDismiss = remember { { _: SwipeToDismissBoxValue -> currentOnAction() } }
    var menuOpen by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)

    Box(modifier = Modifier.testTag(TestTags.personRow(person.id))) {
        SwipeToDismissBox(
            state = state,
            modifier = Modifier.clip(shape),
            enableDismissFromStartToEnd = side == SwipeSide.START_TO_END,
            enableDismissFromEndToStart = side == SwipeSide.END_TO_START,
            onDismiss = onDismiss,
            backgroundContent = { SwipeBackground(side, actionIcon) },
        ) {
            PersonCard(
                person = person,
                // On the card, the node TalkBack focuses: an action on the row's outer box never
                // reaches it.
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .semantics {
                        customActions = listOf(CustomAccessibilityAction(actionLabel) { onAction(); true })
                    },
                onClick = onClick,
                onLongClick = { menuOpen = true },
                trailing = trailing,
            )
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(actionLabel) },
                leadingIcon = { Icon(actionIcon, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    onAction()
                },
            )
        }
    }
}

@Composable
private fun SwipeBackground(side: SwipeSide, icon: ImageVector) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 24.dp),
        contentAlignment = if (side == SwipeSide.END_TO_START) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
