package com.geoviksoft.turnia.e2e.flows

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.geoviksoft.turnia.e2e.infra.Documents
import com.geoviksoft.turnia.e2e.infra.E2eRule
import com.geoviksoft.turnia.e2e.infra.RecordingAnalytics
import com.geoviksoft.turnia.e2e.infra.RecordingTextSharer
import com.geoviksoft.turnia.e2e.infra.SignedInAs
import com.geoviksoft.turnia.e2e.infra.UI_TIMEOUT_MS
import com.geoviksoft.turnia.e2e.infra.awaitLogged
import com.geoviksoft.turnia.e2e.infra.awaitUserProperty
import com.geoviksoft.turnia.e2e.infra.objects
import com.geoviksoft.turnia.e2e.infra.string
import com.geoviksoft.turnia.e2e.infra.strings
import com.geoviksoft.turnia.e2e.robots.AppRobot
import com.geoviksoft.turnia.e2e.robots.CalendarRobot
import com.geoviksoft.turnia.e2e.robots.GroupsRobot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** Paths 8, 9, 10 and 12: dana, who belongs to no group, finds her way into one. */
@RunWith(AndroidJUnit4::class)
class JoinGroupFlowsTest {

    private val compose = createEmptyComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(compose).around(E2eRule(compose, signedInAs = "dana"))

    private val groups = GroupsRobot(compose)

    @Test
    fun createGroup_withItsFirstEventType() {
        groups.createGroup(name = "Quirófano", typeName = "Guardia", typeAcronym = "GQ")

        groups.awaitText("Quirófano")
        val (_, group) = Documents.awaitIn("groups") { it.string("name") == "Quirófano" }
        assertEquals(listOf("dana"), group.strings("memberUids"))
        assertEquals(listOf("dana"), group.strings("adminUids"))
        assertEquals(listOf("Guardia"), group.objects("groupEventTypes").map { it.string("name") })

        val created = compose.awaitLogged("group_created").single()
        assertEquals(1L, created.parameters["type_count"])
        assertEquals(
            "One type created with the group",
            1,
            RecordingAnalytics.named("group_event_type_created").size,
        )
        compose.awaitUserProperty("group_count", "1")
        compose.awaitUserProperty("is_admin", "true")
    }

    @Test
    fun joinByCode_autoApproved() {
        groups.openTab(AppRobot.TAB_GROUPS)
        groups.join("PLA001")

        groups.awaitText("You're in! You're now part of the group.")
        groups.awaitText("Planta")
        Documents.await("groups/planta") { "dana" in it.strings("memberUids") }
    }

    @Test
    fun joinByCode_waitsForAnAdmin() {
        groups.openTab(AppRobot.TAB_GROUPS)
        groups.join("URG001")

        groups.awaitText("Request sent. A group admin has to accept it.")
        groups.awaitText("Waiting for an admin to accept your request.")
        Documents.await("groups/urgencias/joinRequests/dana") { it.string("status") == "pending" }
    }
}

/** Path 12: the invitation link, opened while the app is closed. */
@RunWith(AndroidJUnit4::class)
class InvitationLinkFlowTest {

    private val compose = createEmptyComposeRule()

    private val link = E2eRule(compose, signedInAs = "dana") {
        E2eRule.launchIntent()
            .setAction(Intent.ACTION_VIEW)
            .setData(Uri.parse("https://turnia.club/join/URG001"))
    }

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(compose).around(link)

    @Test
    fun invitationLink_prefillsTheJoinSheet() {
        GroupsRobot(compose).awaitJoinCode("URG001")
        val opened = compose.awaitLogged("invitation_opened").single()
        assertEquals("link", opened.parameters["via"])
    }
}

/** Paths 11, 13, 14 and 40: groups seen from inside. */
@RunWith(AndroidJUnit4::class)
class GroupMembershipFlowsTest {

    private val compose = createEmptyComposeRule()

    private val world = E2eRule(compose)

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(compose).around(world)

    private val groups = GroupsRobot(compose)

    @Test
    @SignedInAs("alice")
    fun adminAcceptsAJoinRequest() {
        groups.openGroupFromSettings("Urgencias")
        groups.awaitText("Irene Ibarra")
        groups.clickDescription("Accept")

        groups.awaitNoText("Irene Ibarra")
        Documents.await("groups/urgencias") { "irene" in it.strings("memberUids") }
        Documents.await("groups/urgencias/joinRequests/irene") { it.string("status") == "accepted" }
    }

    @Test
    @SignedInAs("bruno")
    fun groupCalendar_showsTheWholeGroup() {
        val calendar = CalendarRobot(compose, world.today)

        groups.openGroupCalendar("Urgencias")
        // e1 is alice's, e5 is carla's: bruno's own calendar shows neither.
        calendar.awaitDayShows(world.day(3), "MN")
        calendar.awaitDayShows(world.day(7), "NC")

        groups.clickDescription("View group")
        groups.awaitText("Mañana")
    }

    @Test
    @SignedInAs("carla")
    fun leaveGroup_keepsTheShiftsHeld() {
        groups.openGroupFromSettings("Urgencias")
        groups.leaveGroup()

        val group = Documents.await("groups/urgencias") { "carla" in it.strings("revokedUids") }
        assertFalse("carla" in group.strings("memberUids"))
        groups.openTab(AppRobot.TAB_GROUPS)
        groups.awaitText("No groups around here")
        compose.awaitLogged("group_left")
    }

    @Test
    @SignedInAs("bruno")
    fun anyMemberInvites_withTheShareButton() {
        groups.openGroupFromSettings("Urgencias")
        groups.clickDescription("Invite to group")

        compose.waitUntil(UI_TIMEOUT_MS) { RecordingTextSharer.shared.isNotEmpty() }
        assertTrue(RecordingTextSharer.shared.single().contains("https://turnia.club/join/URG001"))
        compose.awaitLogged("group_invite_shared")
    }
}
