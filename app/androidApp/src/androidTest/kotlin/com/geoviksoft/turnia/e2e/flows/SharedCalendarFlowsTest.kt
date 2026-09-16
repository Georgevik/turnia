package com.geoviksoft.turnia.e2e.flows

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.geoviksoft.turnia.e2e.infra.Documents
import com.geoviksoft.turnia.e2e.infra.E2eRule
import com.geoviksoft.turnia.e2e.infra.strings
import com.geoviksoft.turnia.e2e.robots.AppRobot
import com.geoviksoft.turnia.e2e.robots.CalendarRobot
import com.geoviksoft.turnia.e2e.robots.PeopleRobot
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** Paths 19–20: calendars shared across groups, signed in as alice. */
@RunWith(AndroidJUnit4::class)
class SharedCalendarFlowsTest {

    private val compose = createEmptyComposeRule()

    private val world = E2eRule(compose, signedInAs = "alice")

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(compose).around(world)

    private val people = PeopleRobot(compose)

    @Test
    fun shareMyCalendar_byUsername() {
        people.shareWith(usernamePrefix = "car", username = "carla")

        Documents.await("users/alice") { "carla" in it.strings("calendarSharedWith") }
        people.showSharedByMe()
        people.awaitText("Carla Cruz")
    }

    @Test
    fun viewAColleaguesSharedCalendar() {
        val calendar = CalendarRobot(compose, world.today)

        people.openTab(AppRobot.TAB_PEOPLE)
        people.showSharedWithMe()
        people.click("Bruno Bravo")

        // e2, which bruno covers, served by getSharedCalendar.
        calendar.awaitDayShows(world.day(4), "MN")
    }
}
