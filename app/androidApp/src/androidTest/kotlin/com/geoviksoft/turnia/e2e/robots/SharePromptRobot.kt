package com.geoviksoft.turnia.e2e.robots

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.geoviksoft.turnia.e2e.infra.CountingSharePromptRepository
import com.geoviksoft.turnia.e2e.infra.UI_TIMEOUT_MS
import org.junit.Assert.assertEquals

/** The sheet that asks the user to share Turnia at a milestone of events added. */
internal class SharePromptRobot(compose: ComposeTestRule) : AppRobot(compose) {

    fun awaitCoworkersPrompt() = awaitText(COWORKERS_TITLE)

    fun awaitFriendsPrompt() = awaitText(FRIENDS_TITLE)

    /**
     * No prompt after [eventsAdded] adds. The prompt is decided by the time an add is counted, so
     * once the count is reached and the UI is idle, a prompt that is due is already on screen.
     */
    fun assertNoPrompt(eventsAdded: Int) {
        compose.waitUntil(UI_TIMEOUT_MS) { CountingSharePromptRepository.eventsCounted >= eventsAdded }
        compose.waitForIdle()
        assertEquals("Events counted", eventsAdded, CountingSharePromptRepository.eventsCounted)
        compose.onAllNodes(hasText(SHARE), useUnmergedTree = true).assertCountEquals(0)
    }

    fun share() = click(SHARE)

    fun notNow() = click(NOT_NOW)

    companion object {
        const val COWORKERS_TITLE = "Does your team still swap shifts on WhatsApp?"
        const val FRIENDS_TITLE = "Let your friends know when you're off"
        const val COWORKERS_MESSAGE =
            "I organise my shifts with Turnia. Get it so we can swap them without the WhatsApp mess:"
        const val SHARE = "Share Turnia"
        const val NOT_NOW = "Not now"
    }
}
