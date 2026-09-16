package com.geoviksoft.turnia.e2e.infra

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onFirst

// Long enough for a first read from the emulator, or a callable's cold start, to land on screen.
internal const val UI_TIMEOUT_MS = 30_000L

internal fun ComposeTestRule.awaitNode(
    matcher: SemanticsMatcher,
    useUnmergedTree: Boolean = false,
    timeoutMs: Long = UI_TIMEOUT_MS,
): SemanticsNodeInteraction {
    waitUntil(timeoutMs) {
        onAllNodes(matcher, useUnmergedTree).fetchSemanticsNodes().isNotEmpty()
    }
    return onAllNodes(matcher, useUnmergedTree).onFirst()
}

internal fun ComposeTestRule.awaitNoNode(
    matcher: SemanticsMatcher,
    useUnmergedTree: Boolean = false,
    timeoutMs: Long = UI_TIMEOUT_MS,
) {
    waitUntil(timeoutMs) {
        onAllNodes(matcher, useUnmergedTree).fetchSemanticsNodes().isEmpty()
    }
}
