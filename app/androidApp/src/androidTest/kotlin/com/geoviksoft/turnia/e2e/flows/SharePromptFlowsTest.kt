package com.geoviksoft.turnia.e2e.flows

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.geoviksoft.turnia.e2e.infra.E2eRule
import com.geoviksoft.turnia.e2e.infra.FixedAppConfigRepository
import com.geoviksoft.turnia.e2e.infra.RecordingAnalytics
import com.geoviksoft.turnia.e2e.infra.RecordingTextSharer
import com.geoviksoft.turnia.e2e.robots.CalendarRobot
import com.geoviksoft.turnia.e2e.robots.SharePromptRobot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * Paths 30–37: the prompt that asks alice to share Turnia at milestones of events added. Its
 * milestones are set per test as the console would hold them, and read through the app's parser.
 */
@RunWith(AndroidJUnit4::class)
class SharePromptFlowsTest {

    private val compose = createEmptyComposeRule()

    private val world = E2eRule(compose, signedInAs = "alice")

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(compose).around(world)

    private val calendar by lazy { CalendarRobot(compose, world.today) }
    private val prompt = SharePromptRobot(compose)

    // The fixture's own events sit on days 2–7: every add here gets a day of its own after them.
    private var nextDay = 8

    @Test
    fun shiftsReachAMilestone_theCoworkersPromptShows() {
        FixedAppConfigRepository.sharePrompt(enabled = true, milestones = "[2]")

        addShift()
        prompt.assertNoPrompt()
        addShift()

        prompt.awaitCoworkersPrompt()
        awaitLogged("share_prompt_shown", audience = "coworkers", milestone = 2)
    }

    @Test
    fun oneOffsReachAMilestone_theFriendsPromptShows() {
        FixedAppConfigRepository.sharePrompt(enabled = true, milestones = "[2]")

        addOneOff("Dinner")
        addOneOff("Cinema")

        prompt.awaitFriendsPrompt()
        awaitLogged("share_prompt_shown", audience = "friends", milestone = 2)
    }

    @Test
    fun mixedUse_theEventThatReachesTheMilestoneDecides() {
        FixedAppConfigRepository.sharePrompt(enabled = true, milestones = "[2]")

        addShift()
        addOneOff("Dinner")

        prompt.awaitFriendsPrompt()
    }

    @Test
    fun sharing_handsOutTheTaggedLink() {
        FixedAppConfigRepository.sharePrompt(enabled = true, milestones = "[2]")
        addShift()
        addShift()
        prompt.awaitCoworkersPrompt()

        prompt.share()

        compose.waitUntil(TIMEOUT_MS) { RecordingTextSharer.shared.isNotEmpty() }
        assertEquals(
            listOf(
                "${SharePromptRobot.COWORKERS_MESSAGE} https://turnia.club/" +
                    "?utm_source=turnia_share&utm_medium=share_prompt&utm_campaign=coworkers"
            ),
            RecordingTextSharer.shared,
        )
        awaitLogged("share_prompt_shared", audience = "coworkers", milestone = 2)
        prompt.awaitNoText(SharePromptRobot.SHARE)
    }

    @Test
    fun notNow_doesNotStopTheNextMilestone() {
        FixedAppConfigRepository.sharePrompt(enabled = true, milestones = "[2, 4]")
        addShift()
        addShift()
        prompt.awaitCoworkersPrompt()

        prompt.notNow()
        awaitLogged("share_prompt_dismissed", audience = "coworkers", milestone = 2)
        addShift()
        prompt.assertNoPrompt()
        addShift()

        prompt.awaitCoworkersPrompt()
        awaitLogged("share_prompt_shown", audience = "coworkers", milestone = 4)
        assertEquals("Each milestone is shown once", 2, RecordingAnalytics.named("share_prompt_shown").size)
    }

    @Test
    fun editingAnEvent_doesNotCount() {
        FixedAppConfigRepository.sharePrompt(enabled = true, milestones = "[2]")
        val day = world.day(nextDay++)

        calendar.addOneOff(day, "Dinner")
        calendar.editOneOff(day, "Dinner", "Late dinner")
        calendar.editOneOff(day, "Late dinner", "Later dinner")

        prompt.assertNoPrompt()
    }

    @Test
    fun theFlagOff_neverShowsThePrompt() {
        FixedAppConfigRepository.sharePrompt(enabled = false, milestones = "[2]")

        repeat(3) { addShift() }

        prompt.assertNoPrompt()
        assertTrue(RecordingAnalytics.named("share_prompt_shown").isEmpty())
    }

    @Test
    fun malformedMilestones_neverShowThePrompt() {
        FixedAppConfigRepository.sharePrompt(enabled = true, milestones = "2, 4")

        repeat(3) { addShift() }

        prompt.assertNoPrompt()
        assertTrue(RecordingAnalytics.named("share_prompt_shown").isEmpty())
    }

    /** A group shift: an event with a type. Picking the type closes the day sheet by itself. */
    private fun addShift() {
        calendar.openDay(world.day(nextDay++))
        calendar.addEventOfType("manana")
    }

    private fun addOneOff(name: String) = calendar.addOneOff(world.day(nextDay++), name)

    private fun awaitLogged(name: String, audience: String, milestone: Int) {
        val expected = mapOf("audience" to audience, "milestone" to milestone.toLong())
        compose.waitUntil(TIMEOUT_MS) {
            RecordingAnalytics.named(name).any { it.parameters == expected }
        }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}
