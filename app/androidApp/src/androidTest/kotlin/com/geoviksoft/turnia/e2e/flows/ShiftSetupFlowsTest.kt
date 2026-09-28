package com.geoviksoft.turnia.e2e.flows

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.geoviksoft.turnia.e2e.infra.Documents
import com.geoviksoft.turnia.e2e.infra.E2eRule
import com.geoviksoft.turnia.e2e.infra.FirestoreRest
import com.geoviksoft.turnia.e2e.infra.RecordingAnalytics
import com.geoviksoft.turnia.e2e.infra.SignedInAs
import com.geoviksoft.turnia.e2e.infra.awaitLogged
import com.geoviksoft.turnia.e2e.infra.awaitNode
import com.geoviksoft.turnia.e2e.infra.scrollAndClick
import com.geoviksoft.turnia.e2e.infra.string
import com.geoviksoft.turnia.e2e.robots.AppRobot
import com.geoviksoft.turnia.e2e.robots.CalendarRobot
import com.geoviksoft.turnia.e2e.robots.GroupsRobot
import com.geoviksoft.turnia.e2e.robots.ShiftSetupRobot
import com.geoviksoft.turnia.e2e.robots.SignInRobot
import com.geoviksoft.turnia.ui.system.TestTags
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** Paths 41–45: the shift setup after sign-in, owed only to an account with nothing to reuse. */
@RunWith(AndroidJUnit4::class)
class ShiftSetupFlowsTest {

    private val compose = createEmptyComposeRule()

    private val world = E2eRule(compose, shiftSetupSettled = false)

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(compose).around(world)

    private val setup = ShiftSetupRobot(compose)

    @Test
    fun aNewAccount_landsOnTheSetupAndConfirmsTheDefaults() {
        SignInRobot(compose).createAccount("Nuria Nueva", "nuria@e2e.turnia.club", PASSWORD)

        setup.awaitSetup()
        setup.confirm()

        setup.awaitNoSetup()
        setup.awaitText(ShiftSetupRobot.HINT)
        val uid = checkNotNull(Firebase.auth.currentUser?.uid)
        val types = awaitTypes(uid, count = 3)
        assertEquals(
            listOf(
                Shift("Morning", "M", "08:00", "15:00"),
                Shift("Afternoon", "A", "15:00", "22:00"),
                Shift("Night", "N", "22:00", "08:00"),
            ),
            types.sortedBy { it.start },
        )
        assertEquals("onboarding", compose.awaitLogged("onboard_shift_shown").single().parameters["via"])
        val completed = compose.awaitLogged("onboard_shift_completed").single().parameters
        assertEquals("false", completed["interacted"])
        assertEquals(3L, completed["type_count"])
        assertEquals(0L, completed["custom_type_count"])
        assertEquals(3, compose.awaitLogged("personal_event_type_created", count = 3).size)
    }

    @Test
    @SignedInAs("nuevo")
    fun editingTimesAndAddingAShift_createsWhatIsOnTheList() {
        setup.awaitSetup()
        setup.times(row = 0, start = "0700", end = "1400")
        setup.toggle(row = 2)
        setup.toggle(row = 4)
        setup.addCustom("On call", "OC")
        setup.confirm()

        setup.awaitNoSetup()
        val types = awaitTypes("nuevo", count = 4).associateBy { it.name }
        assertEquals(Shift("Morning", "M", "07:00", "14:00"), types["Morning"])
        assertEquals(Shift("24h duty", "24H", "08:00", "08:00"), types["24h duty"])
        assertEquals(Shift("On call", "OC", null, null), types["On call"])
        assertNull("Night was deselected", types["Night"])
        val completed = compose.awaitLogged("onboard_shift_completed").single().parameters
        assertEquals("true", completed["interacted"])
        assertEquals(4L, completed["type_count"])
        assertEquals(1L, completed["custom_type_count"])

        // A shift that ends at or before it starts ends the next day, and says so.
        setup.openTab(AppRobot.TAB_SETTINGS)
        setup.click("My shifts")
        setup.awaitText("08:00 – 08:00 +1")
    }

    @Test
    @SignedInAs("nuevo")
    fun skipping_isRememberedOnTheDevice() {
        setup.awaitSetup()
        setup.skip()

        setup.awaitNoSetup()
        setup.awaitMain()
        assertEquals("false", compose.awaitLogged("onboard_shift_skipped").single().parameters["interacted"])
        assertTrue(FirestoreRest.list("users/nuevo/personalEventTypes").isEmpty())

        world.relaunch()

        setup.awaitMain()
        setup.awaitNoSetup()
        assertEquals(1, RecordingAnalytics.named("onboard_shift_shown").size)
    }

    @Test
    @SignedInAs("alice")
    fun anAccountWithShifts_neverSeesIt() {
        setup.awaitMain()
        setup.awaitSettled()

        setup.awaitNoSetup()
        assertTrue(RecordingAnalytics.named("onboard_shift_shown").isEmpty())
    }

    @Test
    @SignedInAs("bruno")
    fun aGroupMember_neverSeesIt() {
        setup.awaitMain()
        setup.awaitSettled()

        setup.awaitNoSetup()
        assertTrue(RecordingAnalytics.named("onboard_shift_shown").isEmpty())
    }

    private data class Shift(val name: String?, val acronym: String?, val start: String?, val end: String?)

    private fun awaitTypes(uid: String, count: Int): List<Shift> {
        Documents.awaitIn("users/$uid/personalEventTypes") { true }
        compose.waitUntil(TYPES_TIMEOUT_MS) { FirestoreRest.list("users/$uid/personalEventTypes").size >= count }
        return FirestoreRest.list("users/$uid/personalEventTypes").values.map {
            Shift(it.string("name"), it.string("acronym"), it.string("startTime"), it.string("endTime"))
        }
    }

    private companion object {
        const val PASSWORD = "Turnia-e2e-1"
        const val TYPES_TIMEOUT_MS = 30_000L
    }
}

/** Path 46: whoever arrives through an invitation joins a group that brings its own types. */
@RunWith(AndroidJUnit4::class)
class ShiftSetupInvitationFlowTest {

    private val compose = createEmptyComposeRule()

    private val link = E2eRule(compose, signedInAs = "nuevo", shiftSetupSettled = false) {
        E2eRule.launchIntent()
            .setAction(Intent.ACTION_VIEW)
            .setData(Uri.parse("https://turnia.club/join/URG001"))
    }

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(compose).around(link)

    @Test
    fun anInvitationLink_goesToTheJoinSheetInstead() {
        GroupsRobot(compose).awaitJoinCode("URG001")

        ShiftSetupRobot(compose).awaitNoSetup()
        assertTrue(RecordingAnalytics.named("onboard_shift_shown").isEmpty())
    }
}

/** Paths 47–50: adding to a day starts from the user's shifts, and a one-off is the exception. */
@RunWith(AndroidJUnit4::class)
class ShiftAddPaneFlowsTest {

    private val compose = createEmptyComposeRule()

    private val world = E2eRule(compose)

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(compose).around(world)

    private val calendar by lazy { CalendarRobot(compose, world.today) }
    private val setup = ShiftSetupRobot(compose)

    @Test
    @SignedInAs("nuevo")
    fun noShiftsYet_theEmptyStateOpensTheSetupAndBringsTheShiftsBack() {
        val date = world.day(9)
        calendar.openDay(date)
        calendar.clickDescription("Add event")
        calendar.await(hasTestTag(TestTags.ADD_PANE_EMPTY_SHIFTS))

        calendar.click(ShiftSetupRobot.CREATE_MY_SHIFTS)
        setup.awaitSetup()
        setup.confirm()

        setup.awaitNoSetup()
        assertEquals("add_pane", compose.awaitLogged("onboard_shift_shown").single().parameters["via"])
        assertEquals("add_pane", compose.awaitLogged("onboard_shift_completed").single().parameters["via"])
        setup.awaitText(ShiftSetupRobot.HINT)
        calendar.openDay(date)
        calendar.clickDescription("Add event")
        calendar.pickShiftLabelled("M")
        calendar.awaitDayShows(date, "M")
        Documents.awaitIn("users/nuevo/personalEvents") { it.string("date") == date.toString() }
        assertEquals("typed", compose.awaitLogged("first_event_added").single().parameters["kind"])
    }

    @Test
    @SignedInAs("alice")
    fun shiftsComeFirst_andOneOffsLast() {
        calendar.openDay(world.day(9))
        calendar.clickDescription("Add event")

        val shifts = compose.awaitNode(hasTestTag(TestTags.ADD_PANE_SHIFTS)).fetchSemanticsNode().boundsInRoot
        val other = compose.awaitNode(hasTestTag(TestTags.ADD_PANE_OTHER_EVENT)).fetchSemanticsNode().boundsInRoot
        assertTrue("The shifts sit above the one-off entry", shifts.top < other.top)

        calendar.openOtherEvent()
        calendar.await(hasSetTextAction() and hasText("Name"))
    }

    @Test
    @SignedInAs("alice")
    fun aOneOff_isSavedAsAShiftAndStaysAsItWas() {
        val date = world.day(9)
        calendar.addOneOff(date, "Guardia")
        val (oneOffId, oneOff) = Documents.awaitIn("users/alice/personalOneOffEvents") { it.string("name") == "Guardia" }

        calendar.openOneOff(date, "Guardia")
        compose.awaitNode(hasTestTag(TestTags.ONE_OFF_SAVE_AS_SHIFT)).scrollAndClick()
        calendar.type("Calendar abbreviation", "G")
        calendar.click("Save")

        val (_, type) = Documents.awaitIn("users/alice/personalEventTypes") { it.string("name") == "Guardia" }
        assertEquals("G", type.string("acronym"))
        assertEquals("09:00", type.string("startTime"))
        assertEquals("10:00", type.string("endTime"))
        assertEquals(oneOff.string("color"), type.string("color"))
        compose.awaitLogged("personal_event_type_created")
        assertEquals("Guardia", Documents.get("users/alice/personalOneOffEvents/$oneOffId").string("name"))
    }

    @Test
    @SignedInAs("nuevo")
    fun theFirstEventAdded_reportsItWasAOneOff() {
        calendar.addOneOff(world.day(9), "Cena")
        assertEquals("one_off", compose.awaitLogged("first_event_added").single().parameters["kind"])

        calendar.addOneOff(world.day(10), "Cine")
        compose.awaitLogged("one_off_event_created", count = 2)

        assertEquals(1, RecordingAnalytics.named("first_event_added").size)
    }
}
