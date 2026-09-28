package com.geoviksoft.turnia.e2e.robots

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.geoviksoft.turnia.e2e.infra.CountingTeamPromptRepository
import com.geoviksoft.turnia.e2e.infra.UI_TIMEOUT_MS
import org.junit.Assert.assertEquals

/** The sheet that asks a user in no group whether they work with a team. */
internal class TeamPromptRobot(compose: ComposeTestRule) : AppRobot(compose) {

    fun awaitPrompt() = awaitText(TITLE)

    fun awaitNoPrompt() = awaitNoText(TITLE)

    /**
     * No prompt after [eventsAdded] adds. The team prompt is decided after the share prompt counts
     * an add, server read included; once that is done and the UI is idle, a due prompt is on screen.
     */
    fun assertNoPrompt(eventsAdded: Int) {
        compose.waitUntil(UI_TIMEOUT_MS) { CountingTeamPromptRepository.eventsDecided >= eventsAdded }
        compose.waitForIdle()
        assertEquals("Events decided", eventsAdded, CountingTeamPromptRepository.eventsDecided)
        compose.onAllNodes(hasText(TITLE), useUnmergedTree = true).assertCountEquals(0)
    }

    fun createGroup() = click(CREATE)

    fun haveACode() = click(JOIN)

    fun notNow() = click(NOT_NOW)

    companion object {
        const val TITLE = "Do you work with a team?"
        const val CREATE = "Create a group"
        const val JOIN = "I have a code"
        const val NOT_NOW = "Not now"
    }
}
