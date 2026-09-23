package com.geoviksoft.turnia.e2e.flows

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
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

/**
 * Paths 19–20 and 22–25: calendars shared across groups, signed in as alice. Bruno and dana both
 * share theirs with her, so hiding one still leaves a list to check the other against.
 */
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

    @Test
    fun hideASharedCalendar_bySwipe() {
        people.openTab(AppRobot.TAB_PEOPLE)
        people.showSharedWithMe()
        people.hideBySwipe("bruno")

        people.awaitText(PeopleRobot.CALENDAR_HIDDEN)
        people.awaitNoText("Bruno Bravo")
        people.awaitText("Dana Doe")
        Documents.await(PREFERENCES) { "bruno" in it.strings(HIDDEN) }

        people.showHidden(count = 1)
        people.awaitText("Bruno Bravo")
        people.awaitNoText("Dana Doe")
    }

    @Test
    fun undoHidingASharedCalendar() {
        people.openTab(AppRobot.TAB_PEOPLE)
        people.showSharedWithMe()
        people.hideBySwipe("bruno")
        people.awaitNoText("Bruno Bravo")

        people.undo()

        people.awaitText("Bruno Bravo")
        people.awaitNoHiddenChip()
        Documents.await(PREFERENCES) { "bruno" !in it.strings(HIDDEN) }
    }

    @Test
    fun hideByLongPress_thenShowItAgain() {
        people.openTab(AppRobot.TAB_PEOPLE)
        people.showSharedWithMe()
        people.hideByLongPress("bruno")
        people.awaitNoText("Bruno Bravo")
        Documents.await(PREFERENCES) { "bruno" in it.strings(HIDDEN) }

        people.showHidden(count = 1)
        people.unhideByButton()

        // The chip goes with its last calendar, and the list falls back to the visible ones.
        people.awaitNoHiddenChip()
        people.awaitText("Bruno Bravo")
        people.awaitText("Dana Doe")
        Documents.await(PREFERENCES) { "bruno" !in it.strings(HIDDEN) }
    }

    @Test
    fun hideEverySharedCalendar() {
        people.openTab(AppRobot.TAB_PEOPLE)
        people.showSharedWithMe()
        people.hideBySwipe("bruno")
        people.awaitNoText("Bruno Bravo")
        people.hideBySwipe("dana")

        people.awaitText(PeopleRobot.ALL_HIDDEN)
        Documents.await(PREFERENCES) { it.strings(HIDDEN).containsAll(listOf("bruno", "dana")) }
        people.showHidden(count = 2)
        people.awaitText("Bruno Bravo")
        people.awaitText("Dana Doe")
    }
}

/**
 * Path 26: a calendar hidden on another device, or before a reinstall, is read back from
 * Firestore — the `hidden-calendar` fixture seeds dana as already hidden.
 */
@RunWith(AndroidJUnit4::class)
class HiddenSharedCalendarFlowsTest {

    private val compose = createEmptyComposeRule()

    private val world = E2eRule(compose, signedInAs = "alice", fixtures = listOf("base", "hidden-calendar"))

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(compose).around(world)

    private val people = PeopleRobot(compose)

    @Test
    fun aHiddenCalendarStaysHidden_andSwipesBack() {
        people.openTab(AppRobot.TAB_PEOPLE)
        people.showSharedWithMe()
        people.awaitText("Bruno Bravo")
        people.awaitNoText("Dana Doe")

        people.showHidden(count = 1)
        people.awaitText("Dana Doe")
        people.unhideBySwipe("dana")

        people.awaitNoHiddenChip()
        people.awaitText("Dana Doe")
        Documents.await(PREFERENCES) { "dana" !in it.strings(HIDDEN) }
    }
}

private const val PREFERENCES = "users/alice/private/preferences"
private const val HIDDEN = "hiddenSharedCalendars"
