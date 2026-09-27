package com.geoviksoft.turnia.e2e.robots

import android.os.SystemClock
import androidx.compose.ui.test.junit4.ComposeTestRule

/** The sheet that asks the user to share Turnia at a milestone of events added. */
internal class SharePromptRobot(compose: ComposeTestRule) : AppRobot(compose) {

    fun awaitCoworkersPrompt() = awaitText(COWORKERS_TITLE)

    fun awaitFriendsPrompt() = awaitText(FRIENDS_TITLE)

    /** Nothing shows for a while: the prompt is raised right after an add, so a few seconds is plenty. */
    fun assertNoPrompt() {
        val deadline = SystemClock.uptimeMillis() + QUIET_MS
        while (SystemClock.uptimeMillis() < deadline) {
            compose.waitForIdle()
            SystemClock.sleep(POLL_MS)
        }
        awaitNoText(SHARE)
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
        private const val QUIET_MS = 3_000L
        private const val POLL_MS = 250L
    }
}
