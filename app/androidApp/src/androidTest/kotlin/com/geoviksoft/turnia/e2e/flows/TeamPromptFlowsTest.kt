package com.geoviksoft.turnia.e2e.flows

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.geoviksoft.turnia.e2e.infra.E2eRule
import com.geoviksoft.turnia.e2e.infra.FixedAppConfigRepository
import com.geoviksoft.turnia.e2e.infra.RecordingAnalytics
import com.geoviksoft.turnia.e2e.infra.SignedInAs
import com.geoviksoft.turnia.e2e.infra.awaitLogged
import com.geoviksoft.turnia.e2e.infra.awaitNode
import com.geoviksoft.turnia.e2e.robots.AppRobot
import com.geoviksoft.turnia.e2e.robots.CalendarRobot
import com.geoviksoft.turnia.e2e.robots.SharePromptRobot
import com.geoviksoft.turnia.e2e.robots.TeamPromptRobot
import com.geoviksoft.turnia.ui.system.TestTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * The prompt that asks a user in no group whether they work with a team. dana belongs to no group;
 * alice is a member of Urgencias. The threshold is set per test as the console would hold it.
 */
@RunWith(AndroidJUnit4::class)
class TeamPromptFlowsTest {

    private val compose = createEmptyComposeRule()

    private val world = E2eRule(compose, signedInAs = "dana")

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(compose).around(world)

    private val calendar by lazy { CalendarRobot(compose, world.today) }
    private val prompt = TeamPromptRobot(compose)

    private var nextDay = 8

    @Test
    fun reachingTheThreshold_inNoGroup_showsThePrompt() {
        FixedAppConfigRepository.teamPrompt(enabled = true, threshold = 2)

        addOneOff("Dinner")
        prompt.assertNoPrompt(eventsAdded = 1)
        addOneOff("Cinema")

        prompt.awaitPrompt()
        compose.awaitLogged("onboard_team_shown")
    }

    @Test
    fun iHaveACode_opensAnEmptyJoinSheetOnTheGroupsTab() {
        FixedAppConfigRepository.teamPrompt(enabled = true, threshold = 1)
        addOneOff("Dinner")
        prompt.awaitPrompt()

        prompt.haveACode()

        compose.awaitNode(hasTestTag(TestTags.JOIN_CODE_FIELD))
        prompt.awaitNoPrompt()
        assertEquals("join", answer())
    }

    @Test
    fun createAGroup_opensTheNewGroupForm() {
        FixedAppConfigRepository.teamPrompt(enabled = true, threshold = 1)
        addOneOff("Dinner")
        prompt.awaitPrompt()

        prompt.createGroup()

        prompt.awaitText("New group")
        assertEquals("create", answer())
    }

    @Test
    fun notNow_neverShowsItAgain() {
        FixedAppConfigRepository.teamPrompt(enabled = true, threshold = 1)
        addOneOff("Dinner")
        prompt.awaitPrompt()

        prompt.notNow()
        assertEquals("dismissed", answer())
        addOneOff("Cinema")

        prompt.assertNoPrompt(eventsAdded = 2)
        assertEquals("Shown once per device", 1, RecordingAnalytics.named("onboard_team_shown").size)
    }

    @Test
    @SignedInAs("alice")
    fun aGroupMember_neverSeesIt() {
        FixedAppConfigRepository.teamPrompt(enabled = true, threshold = 1)

        addOneOff("Dinner")
        addOneOff("Cinema")

        prompt.assertNoPrompt(eventsAdded = 2)
        assertTrue(RecordingAnalytics.named("onboard_team_shown").isEmpty())
    }

    @Test
    fun theFlagOff_neverShowsThePrompt() {
        FixedAppConfigRepository.teamPrompt(enabled = false, threshold = 1)

        addOneOff("Dinner")

        prompt.assertNoPrompt(eventsAdded = 1)
    }

    @Test
    fun dueWithAShareMilestone_theTeamPromptWinsAndTheShareWaits() {
        FixedAppConfigRepository.teamPrompt(enabled = true, threshold = 1)
        FixedAppConfigRepository.sharePrompt(enabled = true, milestones = "[1]")

        addOneOff("Dinner")

        prompt.awaitPrompt()
        compose.onAllNodes(hasText(SharePromptRobot.FRIENDS_TITLE), useUnmergedTree = true)
            .assertCountEquals(0)

        prompt.notNow()
        prompt.awaitNoPrompt()
        compose.waitForIdle()
        compose.onAllNodes(hasText(SharePromptRobot.FRIENDS_TITLE), useUnmergedTree = true)
            .assertCountEquals(0)

        prompt.openTab(AppRobot.TAB_GROUPS)
        prompt.openTab(AppRobot.TAB_CALENDAR)

        SharePromptRobot(compose).awaitFriendsPrompt()
    }

    private fun addOneOff(name: String) = calendar.addOneOff(world.day(nextDay++), name)

    private fun answer(): Any? =
        compose.awaitLogged("onboard_team_answered").single().parameters["choice"]
}
