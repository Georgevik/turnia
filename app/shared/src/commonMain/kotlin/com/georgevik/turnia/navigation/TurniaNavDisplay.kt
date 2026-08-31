package com.georgevik.turnia.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay

private const val TRANSITION_DURATION_MS = 200

/**
 * The same fade for forward, pop and predictive back. The last one matters: predictive/system back
 * uses `predictivePopTransitionSpec`, and NavDisplay's default there is a zoom-out — leaving it
 * unset is why pressing the physical back button animated differently from an in-app back.
 */
private fun turniaContentTransform(): ContentTransform =
    fadeIn() togetherWith fadeOut(animationSpec = tween(TRANSITION_DURATION_MS))

@Composable
fun <T : Any> defaultNavEntryDecorators(): List<NavEntryDecorator<T>> = listOf(
    rememberSaveableStateHolderNavEntryDecorator(),
    rememberViewModelStoreNavEntryDecorator(),
)

/** [NavDisplay] over an owned [backStack], applying Turnia's default decorators and transitions. */
@Composable
fun <T : Any> TurniaNavDisplay(
    backStack: List<T>,
    modifier: Modifier = Modifier,
    entryDecorators: List<NavEntryDecorator<T>> = defaultNavEntryDecorators(),
    entryProvider: (T) -> NavEntry<T>,
) {
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        entryDecorators = entryDecorators,
        transitionSpec = { turniaContentTransform() },
        popTransitionSpec = { turniaContentTransform() },
        predictivePopTransitionSpec = { turniaContentTransform() },
        entryProvider = entryProvider,
    )
}

/** [NavDisplay] over pre-decorated [entries] (e.g. the concatenated multi-back-stack tabs). */
@Composable
fun <T : Any> TurniaNavDisplay(
    entries: List<NavEntry<T>>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavDisplay(
        entries = entries,
        modifier = modifier,
        onBack = onBack,
        transitionSpec = { turniaContentTransform() },
        popTransitionSpec = { turniaContentTransform() },
        predictivePopTransitionSpec = { turniaContentTransform() },
    )
}
