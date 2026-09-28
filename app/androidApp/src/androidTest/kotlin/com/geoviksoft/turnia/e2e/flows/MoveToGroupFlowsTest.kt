package com.geoviksoft.turnia.e2e.flows

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.geoviksoft.turnia.e2e.infra.Documents
import com.geoviksoft.turnia.e2e.infra.E2eRule
import com.geoviksoft.turnia.e2e.infra.RecordingAnalytics
import com.geoviksoft.turnia.e2e.infra.awaitLogged
import com.geoviksoft.turnia.e2e.infra.boolean
import com.geoviksoft.turnia.e2e.infra.string
import com.geoviksoft.turnia.e2e.robots.AppRobot
import com.geoviksoft.turnia.e2e.robots.CalendarRobot
import com.geoviksoft.turnia.e2e.robots.MoveToGroupRobot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * Alice moves her personal "Curso" shifts into Urgencias, her only group, so the group step is
 * skipped. Of its four events, `p2` falls on the day she holds `e4` and `p0` is older than the window.
 */
@RunWith(AndroidJUnit4::class)
class MoveToGroupFlowsTest {

    private val compose = createEmptyComposeRule()

    private val world = E2eRule(compose, signedInAs = "alice")

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(compose).around(world)

    private val calendar by lazy { CalendarRobot(compose, world.today) }
    private val move = MoveToGroupRobot(compose)

    @Test
    fun moveOnlyThisOne() {
        calendar.openDay(world.day(2))
        move.startMove("p1")
        move.pickType("manana")
        move.onlyThisOne()

        move.awaitText("1 moved")
        val shift = Documents.await("groups/urgencias/events/p1") { it.string("groupEventTypeId") == "manana" }
        assertEquals("alice", shift.string("ownerId"))
        assertEquals("alice", shift.string("assigneeId"))
        assertEquals(false, shift.boolean("onSwap"))
        assertEquals(world.day(2).toString(), shift.string("date"))
        Documents.await("users/alice/personalEvents/p1") { it.boolean("isDeleted") == true }
        assertEquals(false, Documents.get("users/alice/personalEventTypes/curso").boolean("isDeleted"))

        val moved = compose.awaitLogged("personal_events_moved").single()
        assertEquals(mapOf<String, Any>("scope" to "one", "event_count" to 1L, "skipped_count" to 0L), moved.parameters)
        assertTrue(RecordingAnalytics.named("group_event_created").isEmpty())
        assertTrue(RecordingAnalytics.named("personal_event_deleted").isEmpty())

        move.done()
        calendar.back()
        calendar.awaitDayShows(world.day(2), "MN")
    }

    @Test
    fun moveAllOfThem_skipsTheTakenDayAndDeletesTheType() {
        calendar.openDay(world.day(4))
        move.startMove("p3")
        move.pickType("manana")
        move.allOfThem()

        move.awaitText("2 moved, 1 already had a shift")
        Documents.await("users/alice/personalEventTypes/curso") { it.boolean("isDeleted") == true }
        listOf("p1", "p3").forEach { id ->
            Documents.await("groups/urgencias/events/$id") { it.string("assigneeId") == "alice" }
            assertEquals(true, Documents.get("users/alice/personalEvents/$id").boolean("isDeleted"))
        }
        // p2's day already had e4; p0 is older than the window. Both stay personal.
        assertEquals(false, Documents.get("users/alice/personalEvents/p2").boolean("isDeleted"))
        assertEquals(false, Documents.get("users/alice/personalEvents/p0").boolean("isDeleted"))
        assertTrue("A skipped event creates no shift", documentMissing("groups/urgencias/events/p2"))
        // The note went private, under alice, and never onto the shared shift.
        Documents.await("users/alice/groupEventExtras/p3") { it.string("notes") == "Traer apuntes" }
        assertTrue("notes" !in Documents.get("groups/urgencias/events/p3"))

        val moved = compose.awaitLogged("personal_events_moved").single()
        assertEquals(mapOf<String, Any>("scope" to "all", "event_count" to 2L, "skipped_count" to 1L), moved.parameters)

        move.done()
        calendar.back()
        // The skipped event still renders with the deleted type.
        calendar.awaitDayShows(world.day(6), "CUR")
        calendar.openTab(AppRobot.TAB_SETTINGS)
        calendar.click("My shifts")
        calendar.awaitNoText("Curso")
    }

    private fun documentMissing(path: String): Boolean =
        runCatching { Documents.get(path) }.isFailure
}
