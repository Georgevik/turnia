package com.geoviksoft.turnia.e2e.infra

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import com.geoviksoft.turnia.core.domain.analytics.AnalyticsEvent

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
 * Clicks the node through its own click action. A tap is aimed at where the node was when it was
 * looked up, on the test thread, and lands later on the main thread: a screen that moved in between
 * — the keyboard coming up, content arriving above — takes the tap somewhere else, and on a slow CI
 * emulator that happens. A node with no click action is still tapped, brought into view first.
 */
internal fun SemanticsNodeInteraction.scrollAndClick(): SemanticsNodeInteraction {
    runCatching { performScrollTo() }
    val clickable = SemanticsActions.OnClick in fetchSemanticsNode().config
    return if (clickable) performSemanticsAction(SemanticsActions.OnClick) else performClick()
}

/**
 * Waits until [name] has been logged [count] times and returns those events. The report follows
 * the backend's answer, not the screen, so it can land after the UI already shows the result.
 */
internal fun ComposeTestRule.awaitLogged(
    name: String,
    count: Int = 1,
    timeoutMs: Long = UI_TIMEOUT_MS,
): List<AnalyticsEvent> {
    waitUntil(timeoutMs) { RecordingAnalytics.named(name).size >= count }
    return RecordingAnalytics.named(name)
}

internal fun ComposeTestRule.awaitUserProperty(
    name: String,
    value: String,
    timeoutMs: Long = UI_TIMEOUT_MS,
) {
    waitUntil(timeoutMs) { RecordingAnalytics.property(name) == value }
}
