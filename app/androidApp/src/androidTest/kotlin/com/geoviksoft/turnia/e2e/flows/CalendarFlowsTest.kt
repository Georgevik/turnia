package com.geoviksoft.turnia.e2e.flows

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.geoviksoft.turnia.e2e.infra.Documents
import com.geoviksoft.turnia.e2e.infra.E2eRule
import com.geoviksoft.turnia.e2e.infra.boolean
import com.geoviksoft.turnia.e2e.infra.string
import com.geoviksoft.turnia.e2e.robots.AppRobot
import com.geoviksoft.turnia.e2e.robots.CalendarRobot
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** Paths 4–7: the user's own calendar, signed in as alice. */
@RunWith(AndroidJUnit4::class)
class CalendarFlowsTest {

    private val compose = createEmptyComposeRule()

    private val world = E2eRule(compose, signedInAs = "alice")

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(compose).around(world)

    private val calendar by lazy { CalendarRobot(compose, world.today) }

    @Test
    fun calendar_showsTheSeededMonth() {
        calendar.awaitMain()

        // p1, alice's personal course, then e1 and e4, her group shifts.
        calendar.awaitDayShows(world.day(2), "CUR")
        calendar.awaitDayShows(world.day(3), "MN")
        calendar.awaitDayShows(world.day(6), "MN")
        // Moving on a month still draws a grid.
        calendar.showMonthOf(world.day(40))
    }

    @Test
    fun addGroupEvent_fromTheDaySheet() {
        val date = world.day(8)

        calendar.openDay(date)
        calendar.addEventOfType("manana")

        calendar.awaitDayShows(date, "MN")
        Documents.awaitIn("groups/urgencias/events") {
            it.string("date") == date.toString() &&
                it.string("ownerId") == "alice" &&
                it.string("assigneeId") == "alice" &&
                it.string("groupEventTypeId") == "manana"
        }
    }

    @Test
    fun createPersonalType_thenAddAnEventWithANote() {
        val date = world.day(9)

        calendar.openTab(AppRobot.TAB_SETTINGS)
        calendar.click("My events")
        calendar.click("New event")
        calendar.type("Name", "Guardia extra")
        calendar.type("Calendar abbreviation", "GX")
        calendar.click("Save")
        calendar.awaitText("Guardia extra")
        val (typeId, _) = Documents.awaitIn("users/alice/personalEventTypes") {
            it.string("name") == "Guardia extra" && it.string("acronym") == "GX"
        }

        // My events sits above the tabs.
        calendar.back()
        calendar.openTab(AppRobot.TAB_CALENDAR)
        calendar.openDay(date)
        calendar.addEventOfTypeLabelled("GX")
        calendar.awaitDayShows(date, "GX")
        // Matched by type alone: the app writes a personal event's date as an instant, not the
        // YYYY-MM-DD firestore-schema.md documents, and the type is new to this test anyway.
        val (eventId, _) = Documents.awaitIn("users/alice/personalEvents") { it.string("typeId") == typeId }

        calendar.openDay(date)
        calendar.writeNote(eventId, "Traer bata")

        calendar.awaitEventShows(eventId, "Traer bata")
        Documents.await("users/alice/personalEvents/$eventId") { it.string("notes") == "Traer bata" }
    }

    @Test
    fun deleteOwnEvent() {
        calendar.openDay(world.day(6))
        calendar.clickDescriptionInEvent("e4", "Delete event")
        calendar.click("Delete")

        calendar.awaitNoEvent("e4")
        Documents.await("groups/urgencias/events/e4") { it.boolean("isDeleted") == true }
    }
}
