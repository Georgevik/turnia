package com.geoviksoft.turnia.e2e.flows

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.geoviksoft.turnia.e2e.infra.Documents
import com.geoviksoft.turnia.e2e.infra.E2eRule
import com.geoviksoft.turnia.e2e.infra.RecordingAnalytics
import com.geoviksoft.turnia.e2e.infra.RecordingTextSharer
import com.geoviksoft.turnia.e2e.infra.SignedInAs
import com.geoviksoft.turnia.e2e.infra.UI_TIMEOUT_MS
import com.geoviksoft.turnia.e2e.infra.awaitLogged
import com.geoviksoft.turnia.e2e.infra.objects
import com.geoviksoft.turnia.e2e.infra.string
import com.geoviksoft.turnia.e2e.infra.strings
import com.geoviksoft.turnia.e2e.robots.GroupsRobot
import com.geoviksoft.turnia.e2e.robots.GroupsRobot.Companion.ALONE_CARD_TITLE
import com.geoviksoft.turnia.e2e.robots.GroupsRobot.Companion.INVITE_COLLEAGUES
import com.geoviksoft.turnia.e2e.robots.GroupsRobot.Companion.PROPOSED_TYPES
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** dana, in no group, creates one: its types come proposed and it asks her to invite at once. */
@RunWith(AndroidJUnit4::class)
class NewGroupFlowsTest {

    private val compose = createEmptyComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(compose).around(E2eRule(compose, signedInAs = "dana"))

    private val groups = GroupsRobot(compose)

    @Test
    fun createGroup_byNameAlone_hasTheFourProposedTypes() {
        groups.openNewGroupForm()
        groups.type("Group name", "Quirófano")
        groups.click("Create group")
        groups.finishInviteStep()

        groups.awaitText("Quirófano")
        val (_, group) = Documents.awaitIn("groups") { it.string("name") == "Quirófano" }
        val types = group.objects("groupEventTypes")
        assertEquals(PROPOSED_TYPES, types.map { it.string("name") })
        assertEquals(listOf("M", "A", "N", "MA"), types.map { it.string("acronym") })
        assertEquals(listOf("08:00", "15:00"), listOf(types[0].string("startTime"), types[0].string("endTime")))
        assertEquals(4L, compose.awaitLogged("group_created").single().parameters["type_count"])
        compose.awaitLogged("group_event_type_created", count = 4)
    }

    @Test
    fun removingAProposedType_createsTheGroupWithoutIt() {
        groups.openNewGroupForm()
        groups.type("Group name", "Quirófano")
        groups.removeProposedType("Night")
        groups.awaitNoText("Night")
        groups.click("Create group")
        groups.finishInviteStep()

        val (_, group) = Documents.awaitIn("groups") { it.string("name") == "Quirófano" }
        assertEquals(
            listOf("Morning", "Afternoon", "Morning & afternoon"),
            group.objects("groupEventTypes").map { it.string("name") },
        )
    }

    @Test
    fun theInviteStep_sharesTheInvitationLink() {
        groups.openNewGroupForm()
        groups.type("Group name", "Quirófano")
        groups.click("Create group")
        groups.awaitInviteStep()

        groups.click(INVITE_COLLEAGUES)

        compose.waitUntil(UI_TIMEOUT_MS) { RecordingTextSharer.shared.isNotEmpty() }
        val (_, group) = Documents.awaitIn("groups") { it.string("name") == "Quirófano" }
        val code = checkNotNull(group["invitation"]?.jsonObject?.string("code"))
        assertEquals(
            listOf("Join the group “Quirófano” on Turnia: https://turnia.club/join/$code"),
            RecordingTextSharer.shared,
        )
        compose.awaitLogged("group_invite_shared")
    }

    @Test
    fun theInviteStep_backLeavesItLikeDone() {
        groups.openNewGroupForm()
        groups.type("Group name", "Quirófano")
        groups.click("Create group")
        groups.awaitInviteStep()

        groups.back()

        groups.awaitNoText(GroupsRobot.INVITE_STEP_TITLE)
        groups.awaitText("Quirófano")
        assertTrue("Nothing was shared", RecordingAnalytics.named("group_invite_shared").isEmpty())
    }
}

/** dana runs Consultas alone (fixture `solo-group`), and nuevo is waiting to join it. */
@RunWith(AndroidJUnit4::class)
class AloneGroupFlowsTest {

    private val compose = createEmptyComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(compose)
        .around(E2eRule(compose, signedInAs = "dana", fixtures = listOf("base", "solo-group")))

    private val groups = GroupsRobot(compose)

    @Test
    fun aGroupOfOne_asksForTheTeamUntilASecondMemberJoins() {
        groups.openGroupFromSettings("Consultas")
        groups.awaitText(ALONE_CARD_TITLE)

        groups.clickDescription("Accept")

        Documents.await("groups/consultas") { "nuevo" in it.strings("memberUids") }
        groups.awaitNoText(ALONE_CARD_TITLE)
    }

    @Test
    fun theAloneCard_sharesTheInvitationLink() {
        groups.openGroupFromSettings("Consultas")
        groups.awaitText(ALONE_CARD_TITLE)

        groups.click(INVITE_COLLEAGUES)

        compose.waitUntil(UI_TIMEOUT_MS) { RecordingTextSharer.shared.isNotEmpty() }
        assertEquals(
            listOf("Join the group “Consultas” on Turnia: https://turnia.club/join/CON001"),
            RecordingTextSharer.shared,
        )
        compose.awaitLogged("group_invite_shared")
    }

    @Test
    @SignedInAs("alice")
    fun aGroupOfSeveral_showsNoCard() {
        groups.openGroupFromSettings("Urgencias")
        groups.awaitText("Irene Ibarra")

        groups.awaitNoText(ALONE_CARD_TITLE)
    }
}
