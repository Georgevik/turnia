package com.geoviksoft.turnia.e2e.flows

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.geoviksoft.turnia.e2e.infra.Documents
import com.geoviksoft.turnia.e2e.infra.E2eRule
import com.geoviksoft.turnia.e2e.infra.SignedInAs
import com.geoviksoft.turnia.e2e.infra.boolean
import com.geoviksoft.turnia.e2e.infra.objects
import com.geoviksoft.turnia.e2e.infra.string
import com.geoviksoft.turnia.e2e.robots.AppRobot
import com.geoviksoft.turnia.e2e.robots.CalendarRobot
import com.geoviksoft.turnia.e2e.robots.SwapRobot
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * Paths 15–18: offering, taking and giving back shifts. The fixture already holds the earlier
 * links of each chain, so every test acts as a single user.
 */
@RunWith(AndroidJUnit4::class)
class SwapFlowsTest {

    private val compose = createEmptyComposeRule()

    private val world = E2eRule(compose)

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(compose).around(world)

    private val swaps = SwapRobot(compose)
    private val calendar by lazy { CalendarRobot(compose, world.today) }

    @Test
    @SignedInAs("alice")
    fun offerOwnShift() {
        calendar.openDay(world.day(6))
        calendar.toggleSwap("e4")

        val event = Documents.await("groups/urgencias/events/e4") { it.boolean("onSwap") == true }
        assertEquals(emptyList<Any>(), event.objects("history"))

        calendar.back()
        swaps.openMine()
        swaps.awaitEvent("e4")
    }

    @Test
    @SignedInAs("carla")
    fun takeAColleaguesShift() {
        swaps.openColleagues()
        swaps.take("e1")

        val event = Documents.await("groups/urgencias/events/e1") { it.string("assigneeId") == "carla" }
        assertEquals(false, event.boolean("onSwap"))
        val history = event.objects("history")
        assertEquals(1, history.size)
        assertEquals(
            listOf("transferred", "alice", "carla"),
            history.single().let { listOf(it.string("type"), it.string("fromUid"), it.string("toUid")) },
        )
    }

    /** The case the app exists for: A → B → C, all on one event. */
    @Test
    @SignedInAs("carla")
    fun takeAShiftOfferedAgain_extendsTheChain() {
        swaps.openColleagues()
        swaps.take("e3")

        val event = Documents.await("groups/urgencias/events/e3") { it.string("assigneeId") == "carla" }
        assertEquals(
            listOf("alice" to "bruno", "bruno" to "carla"),
            event.objects("history").map { it.string("fromUid") to it.string("toUid") },
        )

        swaps.openTab(AppRobot.TAB_CALENDAR)
        calendar.openDay(world.day(5))
        calendar.awaitEventShows("e3", "Alice Admin")
        calendar.awaitEventShows("e3", "Bruno Bravo")
    }

    @Test
    @SignedInAs("bruno")
    fun giveATakenShiftBack() {
        calendar.openDay(world.day(4))
        calendar.clickDescriptionInEvent("e2", "Give shift back")
        calendar.click("Give back")

        val event = Documents.await("groups/urgencias/events/e2") { it.string("assigneeId") == "alice" }
        assertEquals(true, event.boolean("onSwap"))
        assertEquals("returned", event.objects("history").last().string("type"))
    }
}
