package com.geoviksoft.turnia.e2e.infra

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo

// Long enough for a first read from the emulator, or a callable's cold start, to land on screen.
internal const val UI_TIMEOUT_MS = 30_000L

internal fun ComposeTestRule.awaitNode(
    matcher: SemanticsMatcher,
    useUnmergedTree: Boolean = false,
    timeoutMs: Long = UI_TIMEOUT_MS,
): SemanticsNodeInteraction {
    waitUntil(timeoutMs) { (count(matcher, useUnmergedTree) ?: 0) > 0 }
    return onAllNodes(matcher, useUnmergedTree).onFirst()
}

/** The first node matching [inMergedTree], or failing that [inUnmergedTree]. */
internal fun ComposeTestRule.awaitAnyNode(
    inMergedTree: SemanticsMatcher,
    inUnmergedTree: SemanticsMatcher,
    timeoutMs: Long = UI_TIMEOUT_MS,
): SemanticsNodeInteraction {
    waitUntil(timeoutMs) {
        (count(inMergedTree, false) ?: 0) > 0 || (count(inUnmergedTree, true) ?: 0) > 0
    }
    return if ((count(inMergedTree, false) ?: 0) > 0) {
        onAllNodes(inMergedTree).onFirst()
    } else {
        onAllNodes(inUnmergedTree, useUnmergedTree = true).onFirst()
    }
}

internal fun ComposeTestRule.awaitNoNode(
    matcher: SemanticsMatcher,
    useUnmergedTree: Boolean = false,
    timeoutMs: Long = UI_TIMEOUT_MS,
) {
    waitUntil(timeoutMs) { count(matcher, useUnmergedTree) == 0 }
}

/** Until the activity has called setContent there is no hierarchy at all, which is not "no match". */
private fun ComposeTestRule.count(matcher: SemanticsMatcher, useUnmergedTree: Boolean): Int? =
    runCatching { onAllNodes(matcher, useUnmergedTree).fetchSemanticsNodes().size }.getOrNull()

/**
 * A click lands where the node is drawn, so one scrolled out of view — below the fold, or behind
 * the keyboard — is brought into view first. Nodes outside any scrollable just get clicked.
 */
internal fun SemanticsNodeInteraction.scrollAndClick(): SemanticsNodeInteraction {
    runCatching { performScrollTo() }
    return performClick()
}
