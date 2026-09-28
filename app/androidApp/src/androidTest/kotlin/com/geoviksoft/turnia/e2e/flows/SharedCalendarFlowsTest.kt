package com.geoviksoft.turnia.e2e.flows

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.geoviksoft.turnia.e2e.infra.Documents
import com.geoviksoft.turnia.e2e.infra.E2eRule
import com.geoviksoft.turnia.e2e.infra.strings
import com.geoviksoft.turnia.e2e.robots.AppRobot
import com.geoviksoft.turnia.e2e.robots.CalendarRobot
import com.geoviksoft.turnia.e2e.robots.PeopleRobot
import android.os.SystemClock
import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.FirestoreAudit
import com.geoviksoft.turnia.core.data.sharedcalendar.CachedSharedCalendar
import com.geoviksoft.turnia.core.data.sharedcalendar.SharedCalendarCache
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.e2e.infra.FirestoreRest
import com.geoviksoft.turnia.e2e.infra.string
import com.geoviksoft.turnia.e2e.infra.awaitLogged
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.plusMonth
import kotlinx.datetime.yearMonth
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import androidx.compose.ui.test.junit4.ComposeTestRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import kotlin.time.Instant

/**
 * Paths 19–20, 22–25 and 27–29: calendars shared across groups, signed in as alice. Bruno and dana both
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
        compose.awaitLogged("calendar_shared")
    }

    @Test
    fun viewAColleaguesSharedCalendar() {
        val calendar = CalendarRobot(compose, world.today)

        people.openTab(AppRobot.TAB_PEOPLE)
        people.showSharedWithMe()
        people.click("Bruno Bravo")

        // e2, which bruno covers, served by getSharedCalendar.
        calendar.awaitDayShows(world.day(4), "MN")
        compose.awaitLogged("shared_calendar_viewed")
    }

    /**
     * What a colleague's calendar costs, step by step: one call per month the first time, nothing
     * on the way back or on reopening, and after that one call per change the owner makes, answered
     * with only what changed — and nothing at all while the app is in the background or behind
     * another tab. Each count is checked as a difference, on the owner's marker listener and on the
     * callable, and the device's cache is read back to show where the days came from.
     */
    @Test
    fun aColleaguesCalendar_isCachedAndCaughtUpByTheGap() {
        awaitSeedTriggersSettled(owner = "bruno", seededAt = world.seededAt)
        val thisMonth = world.today.yearMonth
        val nextMonth = thisMonth.plusMonth()
        val shiftMonth = world.day(4).yearMonth

        people.openTab(AppRobot.TAB_PEOPLE)
        people.showSharedWithMe()
        people.click("Bruno Bravo")
        val calendar = CalendarRobot(compose, world.today)

        // Opening asks once, not twice for the same month, and attaches the owner's markers once.
        compose.awaitCalls(1)
        assertTrue("Opening read the owner's markers ${markerReads()} times", markerReads() <= 1)
        // Counted as it goes out, so the entry lands a moment after the count.
        compose.awaitCached(thisMonth) { true }

        calendar.showMonthOf(nextMonth.firstDay)
        compose.awaitCalls(2)
        compose.awaitCached(nextMonth) { true }

        val reads = markerReads()
        calendar.showPreviousMonth()
        compose.assertStill(calls = 2, markerReads = reads)

        people.back()
        people.click("Bruno Bravo")
        val reopened = CalendarRobot(compose, world.today)
        reopened.awaitDayShows(world.day(4), "MN")
        compose.assertStill(calls = 2, markerReads = reads)

        // A change bruno's shift gets on the server wakes the open calendar through his marker.
        val before = checkNotNull(cached(shiftMonth))
        changeEvent("e2") { JsonObject(it + ("onSwap" to JsonPrimitive(true))) }
        compose.awaitCalls(3)
        assertEquals(reads + 1, markerReads())
        val swapped = compose.awaitCached(shiftMonth) { entry ->
            entry.response.groupEvents.any { it.eventId == "e2" && it.onSwap }
        }
        assertTrue("The cursor did not move forward", swapped.cursor!! > before.cursor!!)
        assertEquals(
            "Catching up lost or added shifts it had no news of",
            before.response.groupEvents.map { it.eventId }.toSet(),
            swapped.response.groupEvents.map { it.eventId }.toSet(),
        )

        // The marker trails its event; comparing the two would never settle and call forever.
        compose.assertStill(calls = 3, markerReads = reads + 1)

        // Given away: it leaves bruno's calendar and his cache, while e3, untouched, stays cached.
        // Not looked for on screen: day 5 may be next month, and paging there would catch it up too.
        changeEvent("e2") { JsonObject(it + ("assigneeId" to JsonPrimitive("carla"))) }
        compose.awaitCalls(4)
        reopened.awaitDayDoesNotShow(world.day(4), "MN")
        val handedAway = compose.awaitCached(shiftMonth) { entry ->
            entry.response.groupEvents.none { it.eventId == "e2" }
        }
        assertTrue("e3 was dropped", handedAway.response.groupEvents.any { it.eventId == "e3" })
        compose.assertStill(calls = 4, markerReads = reads + 2)

        // In the background nothing is followed: bruno's next change costs nothing until the user is back.
        world.moveToBackground()
        compose.idleFor(DETACH_MS)
        val readsInBackground = markerReads()
        // e3 is seeded offered for swap, so withdrawing it is a change.
        changeEvent("e3") { JsonObject(it + ("onSwap" to JsonPrimitive(false))) }
        compose.assertStill(calls = 4, markerReads = readsInBackground)
        world.moveToForeground()
        compose.awaitCalls(5)
        compose.awaitCached(shiftMonth) { entry ->
            entry.response.groupEvents.any { it.eventId == "e3" && !it.onSwap }
        }
        assertEquals("Coming back re-attached more than once", readsInBackground + 1, markerReads())

        // Behind another tab the People back stack keeps the calendar, but nothing is followed either.
        people.openTab(AppRobot.TAB_CALENDAR)
        compose.idleFor(DETACH_MS)
        val readsOnAnotherTab = markerReads()
        changeEvent("e3") { JsonObject(it + ("onSwap" to JsonPrimitive(true))) }
        compose.assertStill(calls = 5, markerReads = readsOnAnotherTab)
        people.openTab(AppRobot.TAB_PEOPLE)
        compose.awaitCalls(6)
        compose.awaitCached(shiftMonth) { entry ->
            entry.response.groupEvents.any { it.eventId == "e3" && it.onSwap }
        }

        // Nobody but the server may move the marker the calendar above depends on.
        val refused = try {
            writeOwnSyncField("groupEvents.$thisMonth.updatedAt")
            false
        } catch (_: Exception) {
            true
        }
        if (!refused) fail("A client moved its own groupEvents marker")
        writeOwnSyncField("preferences")
    }

    /** Bruno stops sharing while alice looks: the next call says so, and his months leave her device. */
    @Test
    fun aWithdrawnGrant_erasesTheCachedCalendar() {
        awaitSeedTriggersSettled(owner = "bruno", seededAt = world.seededAt)
        val thisMonth = world.today.yearMonth
        openBrunosCalendar()
        compose.awaitCalls(1)
        compose.awaitCached(thisMonth) { true }

        revokeAlicesGrant()

        compose.awaitCalls(2)
        people.awaitText(NOT_SHARED)
        compose.waitUntil(CALL_TIMEOUT_MS) { cached(thisMonth) == null }
    }

    /** A one-off moved months away is no longer in the window it was cached in, and must leave it. */
    @Test
    fun aOneOffMovedToAnotherMonth_leavesTheCachedMonth() {
        val thisMonth = world.today.yearMonth
        val farMonth = thisMonth.plusMonth().plusMonth().plusMonth()
        writeBrunosOneOff(day = world.today, markedMonths = setOf(thisMonth))
        awaitSeedTriggersSettled(owner = "bruno", seededAt = world.seededAt)
        openBrunosCalendar()
        compose.awaitCalls(1)
        compose.awaitCached(thisMonth) { entry ->
            entry.response.personalOneOffEvents.any { it.eventId == ONE_OFF }
        }

        // Bruno's app stamps the month it leaves as well as the one it lands in.
        writeBrunosOneOff(day = farMonth.firstDay, markedMonths = setOf(thisMonth, farMonth))

        compose.awaitCalls(2)
        compose.awaitCached(thisMonth) { entry ->
            entry.response.personalOneOffEvents.none { it.eventId == ONE_OFF }
        }
    }

    private fun openBrunosCalendar() {
        people.openTab(AppRobot.TAB_PEOPLE)
        people.showSharedWithMe()
        people.click("Bruno Bravo")
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
        compose.awaitLogged("shared_calendar_hidden")

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

/**
 * The seed writes every event, and the Functions emulator stamps each holder's `groupEvents` a
 * moment later. Counting calls before that lands would count the catch-up it causes as well.
 */
private fun awaitSeedTriggersSettled(owner: String, seededAt: Instant) {
    val path = "users/$owner/sync/updates"
    Documents.await(path) { sync ->
        val stamps = sync.objectOrNull("groupEvents")?.values.orEmpty()
        stamps.isNotEmpty() && stamps.all { stamp ->
            val updatedAt = stamp.jsonObject["updatedAt"]?.jsonObject?.string(FirestoreRest.TIMESTAMP)
            // Compared for equality, not order: the stamps come from the host's clock, the seed from
            // the device's, and the two need not agree.
            updatedAt != null && Instant.parse(updatedAt) != seededAt
        }
    }
    // Two shifts in the same month are two triggers; wait for the second as well.
    var last = Documents.get(path)
    var quietSince = SystemClock.uptimeMillis()
    while (SystemClock.uptimeMillis() - quietSince < QUIET_MS) {
        SystemClock.sleep(POLL_MS)
        val current = Documents.get(path)
        if (current != last) {
            last = current
            quietSince = SystemClock.uptimeMillis()
        }
    }
}

private fun calls() = FirestoreAudit.callsTo("getSharedCalendar")

private fun markerReads() = FirestoreAudit.usage("UserSyncFirestore", "sharedSync(snapshots)").serverReads

// Waited through the Compose rule, never with a bare sleep: the test drives the app's frames, and
// a screen that is not composed never asks for anything.
private fun ComposeTestRule.awaitCalls(expected: Int) {
    runCatching { waitUntil(CALL_TIMEOUT_MS) { calls() >= expected } }
        .onFailure { fail("Expected $expected calls, saw ${calls()}") }
    assertEquals("More calls than expected", expected, calls())
}

/** Nothing more is spent for a while: a loop, or a call that should have been served from the device, would show here. */
private fun ComposeTestRule.assertStill(calls: Int, markerReads: Int) {
    idleFor(QUIET_MS)
    assertEquals("getSharedCalendar was called again", calls, calls())
    assertEquals("The owner's markers were read again", markerReads, markerReads())
}

private fun ComposeTestRule.idleFor(millis: Long) {
    val deadline = SystemClock.uptimeMillis() + millis
    while (SystemClock.uptimeMillis() < deadline) {
        waitForIdle()
        SystemClock.sleep(POLL_MS)
    }
}

private fun cache(): SharedCalendarCache = GlobalContext.get().get()

private fun cached(month: YearMonth): CachedSharedCalendar? =
    runBlocking { cache().read(UserId("alice"), UserId("bruno"), month) }

private fun ComposeTestRule.awaitCached(
    month: YearMonth,
    condition: (CachedSharedCalendar) -> Boolean,
): CachedSharedCalendar {
    runCatching { waitUntil(CALL_TIMEOUT_MS) { cached(month)?.let(condition) == true } }
        .onFailure { fail("The cache entry for $month never matched") }
    return checkNotNull(cached(month))
}

/** An Urgencias shift, changed on the server as any other client's write would, `updateAt` stamped by the server. */
private fun changeEvent(eventId: String, change: (JsonObject) -> JsonObject) {
    val path = "groups/urgencias/events/$eventId"
    FirestoreRest.write(mapOf(path to JsonObject(change(Documents.get(path)) + ("updateAt" to FirestoreRest.SERVER_TIMESTAMP))))
}

/** Bruno takes alice off his grant list, with his profile marker in the same commit, as his app does. */
private fun revokeAlicesGrant() {
    val now = FirestoreRest.SERVER_TIMESTAMP
    val profile = Documents.get("users/bruno")
    val sync = Documents.get("users/bruno/sync/updates")
    val sharedWith = profile.strings("calendarSharedWith").filter { it != "alice" }.map(::JsonPrimitive)
    FirestoreRest.write(
        mapOf(
            "users/bruno" to JsonObject(profile + ("calendarSharedWith" to JsonArray(sharedWith)) + ("updateAt" to now)),
            "users/bruno/sync/updates" to JsonObject(sync + ("profile" to now)),
        )
    )
}

/** Bruno's one-off on [day], and the one-off markers of [markedMonths], in one commit, as his app writes them. */
private fun writeBrunosOneOff(day: LocalDate, markedMonths: Set<YearMonth>) {
    val now = FirestoreRest.SERVER_TIMESTAMP
    val month = JsonPrimitive(day.yearMonth.toString())
    val oneOff = buildJsonObject {
        put("name", "Course")
        put("color", "#3366FF")
        put("start", "${day}T09:00")
        put("end", "${day}T10:00")
        put("allDay", false)
        put("yearMonthStart", month)
        put("yearMonthEnd", month)
        put("notes", JsonNull)
        put("isDeleted", false)
        put("updateAt", now)
    }
    val sync = Documents.get("users/bruno/sync/updates")
    val stamped = sync.objectOrNull("personalOneOffEvents").orEmpty() +
        markedMonths.associate { it.toString() to buildJsonObject { put("updatedAt", now) } }
    FirestoreRest.write(
        mapOf(
            "users/bruno/personalOneOffEvents/$ONE_OFF" to oneOff,
            "users/bruno/sync/updates" to JsonObject(sync + ("personalOneOffEvents" to JsonObject(stamped))),
        )
    )
}

private fun writeOwnSyncField(field: String) = runBlocking {
    val firestore = GlobalContext.get().get<FirebaseFirestore>()
    val batch = firestore.batch()
    batch.updateFields(firestore.collection("users/alice/sync").document("updates")) {
        field to Timestamp.ServerTimestamp
    }
    batch.commit()
}

private fun JsonObject.objectOrNull(key: String): JsonObject? = get(key) as? JsonObject

private const val ONE_OFF = "o-bruno"
private const val NOT_SHARED = "This person no longer shares their calendar with you."

private const val CALL_TIMEOUT_MS = 30_000L
private const val QUIET_MS = 5_000L

// The screen lets go 5 s after it stops watching, the owner's listener 30 s after that.
private const val DETACH_MS = 40_000L
private const val POLL_MS = 250L

private const val PREFERENCES = "users/alice/private/preferences"
private const val HIDDEN = "hiddenSharedCalendars"
