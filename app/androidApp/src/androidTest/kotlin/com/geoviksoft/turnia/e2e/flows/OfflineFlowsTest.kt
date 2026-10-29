package com.geoviksoft.turnia.e2e.flows

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.FirestoreAudit
import com.geoviksoft.turnia.core.domain.model.EventTypeId
import com.geoviksoft.turnia.core.domain.model.PersonalEventType
import com.geoviksoft.turnia.core.domain.repository.PersonalEventRepository
import com.geoviksoft.turnia.e2e.infra.Documents
import com.geoviksoft.turnia.e2e.infra.FirestoreRest
import com.geoviksoft.turnia.e2e.infra.E2eRule
import com.geoviksoft.turnia.e2e.infra.Network
import com.geoviksoft.turnia.e2e.infra.boolean
import com.geoviksoft.turnia.e2e.infra.string
import com.geoviksoft.turnia.e2e.robots.CalendarRobot
import dev.gitlive.firebase.firestore.Source
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.datetime.yearMonth
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.context.GlobalContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** The calendar with no connection: what the user sees before the writes reach the server. */
@RunWith(AndroidJUnit4::class)
class OfflineFlowsTest {

    private val compose = createEmptyComposeRule()

    private val world = E2eRule(compose, signedInAs = "alice")

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(compose).around(world)

    private val calendar by lazy { CalendarRobot(compose, world.today) }

    /**
     * The lever itself. Every other test here is worthless if cutting the connection does not cut
     * it, and a silently working `disableNetwork` would make them all pass for the wrong reason.
     */
    @Test
    fun offlineLever_cutsTheConnectionAndRestoresIt() {
        calendar.awaitMain()

        Network.offline {
            try {
                serverRead()
                fail("Expected a server read to fail while the connection is cut")
            } catch (expected: Exception) {
                assertTrue(
                    "Expected an offline failure, got: ${expected.message}",
                    expected.message != null,
                )
            }
        }

        // Restored: the same read the cut refused now reaches the emulator.
        serverRead()
    }

    /** 3.1 — a shift of a personal type, added with nothing to send it to. */
    @Test
    fun offline_addingAShift_showsItAtOnce() {
        calendar.awaitMain()

        // base puts alice's Curso events on +2, +4 and +6, so +1 starts empty.
        val day = world.day(1)
        awaitSeedHeld()
        calendar.awaitDayDoesNotShow(day, "CUR")

        Network.offline {
            calendar.openDay(day)
            calendar.addEventOfTypeLabelled("CUR")
            calendar.awaitDayClosed()

            // From the cache, with nothing uploaded yet.
            calendar.awaitDayShows(day, "CUR")
        }

        // Back online the queued write reaches the emulator on its own.
        Documents.awaitIn("users/alice/personalEvents") { it.string("date") == day.toString() }
    }

    /** 3.2 — a group shift, read through the group's own marker rather than the user's. */
    @Test
    fun offline_addingAGroupShift_showsItAtOnce() {
        calendar.awaitMain()

        // Urgencias' shifts sit on +3 to +7, so +1 holds none of them.
        val day = world.day(1)
        awaitSeedHeld()
        calendar.awaitDayDoesNotShow(day, "MN")

        Network.offline {
            calendar.openDay(day)
            calendar.addEventOfTypeLabelled("MN")
            calendar.awaitDayClosed()

            calendar.awaitDayShows(day, "MN")
        }

        Documents.awaitIn("groups/urgencias/events") { it.string("date") == day.toString() }
    }

    /** 3.3 — a one-off event, which has a reader of its own. */
    @Test
    fun offline_addingAOneOff_showsItAtOnce() {
        calendar.awaitMain()
        val day = world.day(1)

        Network.offline {
            calendar.addOneOff(day, "Dinner")
            calendar.awaitDayShows(day, "Dinner")
        }

        Documents.awaitIn("users/alice/personalOneOffEvents") { it.string("name") == "Dinner" }
    }

    /**
     * 3.3 — and a one-off spanning two months stays on the days of both while offline. Seeded
     * rather than filled in, because setting an end date a month out means driving Material's date
     * picker across a month boundary; what this has to prove is that the reader serves both months
     * from the cache, and an edit-save exercises that on the same path an add does.
     */
    @Test
    fun offline_editingATwoMonthOneOff_showsItInBothMonths() {
        val start = world.today
        val end = start.plus(1, DateTimeUnit.MONTH)
        seedAlicesSpanningOneOff(start, end)

        calendar.awaitMain()
        calendar.awaitDayShows(start, SPANNING)

        Network.offline {
            // Edited on the month it was seeded on, before any assertion pages away from it.
            calendar.editOneOff(start, SPANNING, "Congress week")

            calendar.awaitDayShows(start, "Congress week")
            calendar.awaitDayShows(end, "Congress week")
        }

        Documents.await("users/alice/personalOneOffEvents/$SPANNING_ID") {
            it.string("name") == "Congress week"
        }
    }

    /** 3.4 — a private note on a group shift, which lives under the user, not the group. */
    @Test
    fun offline_writingANoteOnAGroupShift_showsItAtOnce() {
        calendar.awaitMain()

        // e4 is alice's own Urgencias shift, on +6, and base gives it no note.
        calendar.openDay(world.day(6))

        Network.offline {
            calendar.writeNote("e4", "Parking B")
            calendar.awaitEventShows("e4", "Parking B")
        }

        Documents.await("users/alice/groupEventExtras/e4") { it.string("notes") == "Parking B" }
    }

    /**
     * 4.1 — a shift created offline has to reach the types reader too: the chip only appears if it
     * did, and an event of a type the map is missing is dropped before it reaches the grid. Driven
     * through the repository because the only UI route to a new type is the shift setup, which this
     * change leaves alone.
     */
    @Test
    fun offline_creatingAShiftType_offersItAndRendersItsEvents() {
        calendar.awaitMain()
        val day = world.day(1)

        Network.offline {
            val repository: PersonalEventRepository = GlobalContext.get().get()
            // Not awaited: the write resolves on the server's commit, which is what is cut.
            CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                repository.saveEventType(
                    PersonalEventType(
                        id = EventTypeId("t-offline"),
                        name = "On call",
                        color = "#FF8A65",
                        acronym = "OC",
                        description = null,
                        startTime = null,
                        endTime = null,
                    ),
                    isNew = true,
                )
            }

            calendar.openDay(day)
            // Finding this chip at all proves the type reached the reader from the cache.
            calendar.addEventOfTypeLabelled("OC")
            calendar.awaitDayClosed()

            calendar.awaitDayShows(day, "OC")
        }

        Documents.await("users/alice/personalEventTypes/t-offline") {
            it.string("name") == "On call"
        }
    }

    /**
     * 5.4 — the blank-calendar regression. A month whose marker is ahead of this device's cache
     * used to send the reader to the server; offline that read fails, and the terminal catch
     * emptied the whole window, not just that month. The events the device does hold must survive.
     */
    @Test
    fun offline_aMonthTheDeviceNeverSynced_keepsTheRestOfTheCalendar() {
        // A marker for next month with no events behind it: the cache holds nothing for it, so the
        // month reads as behind — the state another of alice's devices would leave.
        markPersonalEventsMonth(world.today.plus(1, DateTimeUnit.MONTH))

        calendar.awaitMain()
        calendar.awaitDayShows(world.day(2), "CUR")
        // Group events load apart from personal ones: they have to be held before going offline.
        calendar.awaitDayShows(world.day(6), "MN")

        Network.goOffline()
        try {
            // Reopened in the same process, so the reader subscribes afresh with no connection.
            world.relaunch()
            val offlineCalendar = CalendarRobot(compose, world.today)
            offlineCalendar.awaitMain()

            offlineCalendar.awaitDayShows(world.day(2), "CUR")
            offlineCalendar.awaitDayShows(world.day(6), "MN")
        } finally {
            Network.goOnline()
        }
    }

    /**
     * 3.6 — and it costs nothing. The whole point of serving the cache is that an offline add is
     * free: cache reads climb, server reads do not move, and the only charge is the write itself.
     */
    @Test
    fun offline_addingAShift_costsNoServerRead() {
        calendar.awaitMain()
        val day = world.day(1)
        // Settled first, so the startup sync's reads are not counted against the offline add.
        calendar.awaitDayShows(world.day(2), "CUR")

        val serverBefore = serverReadsOfPersonalEvents()
        val cacheBefore = cacheReadsOfPersonalEvents()

        Network.offline {
            calendar.openDay(day)
            calendar.addEventOfTypeLabelled("CUR")
            calendar.awaitDayClosed()
            calendar.awaitDayShows(day, "CUR")

            assertEquals(
                "An offline add must cost no server read",
                serverBefore,
                serverReadsOfPersonalEvents(),
            )
            assertTrue(
                "The event came from somewhere: the cache was read again",
                cacheReadsOfPersonalEvents() > cacheBefore,
            )
        }
    }

    private fun serverReadsOfPersonalEvents() =
        FirestoreAudit.usage("PersonalEventFirestore", "events(SERVER)").serverReads

    private fun cacheReadsOfPersonalEvents() =
        FirestoreAudit.usage("PersonalEventFirestore", "events(CACHE)").cachedReads

    /** 6.1 — everything written offline reaches the server once the connection is back, once each. */
    @Test
    fun reconnecting_uploadsEveryOfflineAdd_exactlyOnce() {
        calendar.awaitMain()
        val first = world.day(1)
        val second = world.day(3)
        awaitSeedHeld()

        Network.offline {
            calendar.openDay(first)
            calendar.addEventOfTypeLabelled("CUR")
            calendar.awaitDayClosed()
            calendar.openDay(second)
            calendar.addEventOfTypeLabelled("CUR")
            calendar.awaitDayClosed()

            calendar.awaitDayShows(first, "CUR")
            calendar.awaitDayShows(second, "CUR")
        }

        Documents.awaitIn("users/alice/personalEvents") { it.string("date") == first.toString() }
        Documents.awaitIn("users/alice/personalEvents") { it.string("date") == second.toString() }

        // base seeds p0 to p3; two more and no duplicates of either.
        val stored = FirestoreRest.list("users/alice/personalEvents")
        assertEquals("One document per add, with nothing written twice", 6, stored.size)

        // Still on the calendar once the server has them, neither reverted nor doubled.
        calendar.awaitDayShows(first, "CUR")
        calendar.awaitDayShows(second, "CUR")
    }

    /**
     * 6.1 — and it survives the app being closed and reopened before the connection comes back.
     * Reopened in the same process, so the cache and its queued mutations are the ones the reader
     * finds; surviving an actual process death is Firestore's own guarantee, and the suite clears
     * the app's data between tests, so it cannot be staged here.
     */
    @Test
    fun offline_closedAndReopened_keepsTheEventAndStillUploadsIt() {
        calendar.awaitMain()
        val day = world.day(1)
        awaitSeedHeld()

        Network.goOffline()
        try {
            calendar.openDay(day)
            calendar.addEventOfTypeLabelled("CUR")
            calendar.awaitDayClosed()
            calendar.awaitDayShows(day, "CUR")

            world.relaunch()
            val reopened = CalendarRobot(compose, world.today)
            reopened.awaitMain()
            reopened.awaitDayShows(day, "CUR")
        } finally {
            Network.goOnline()
        }

        Documents.awaitIn("users/alice/personalEvents") { it.string("date") == day.toString() }
    }

    /** 6.2 — an unsent event is rendered as any other, and nothing announces that it is unsent. */
    @Test
    fun offline_anUnsentEventIsIndistinguishable() {
        calendar.awaitMain()
        val day = world.day(1)
        awaitSeedHeld()

        Network.offline {
            calendar.openDay(day)
            calendar.addEventOfTypeLabelled("CUR")
            calendar.awaitDayClosed()
            calendar.awaitDayShows(day, "CUR")

            // The seeded p1 on +2 is the same type: the unsent row carries nothing extra.
            calendar.awaitDayShows(world.day(2), "CUR")

            PENDING_WORDING.forEach { wording ->
                calendar.awaitNoText(wording)
            }
        }
    }

    /** 6.3 — a delete is just as immediate, and is a soft delete once the server has it. */
    @Test
    fun offline_deletingAnEvent_hidesItAtOnce() {
        calendar.awaitMain()
        // Loaded online first: +6 can fall in a month the device has never fetched.
        calendar.awaitDayShows(world.day(6), "MN")

        Network.offline {
            calendar.openDay(world.day(6))
            calendar.eventAction("e4", "Delete event")
            calendar.click("Delete")

            calendar.awaitNoEvent("e4")
        }

        Documents.await("groups/urgencias/events/e4") { it.boolean("isDeleted") == true }
    }

    /**
     * Waits for alice's seeded shifts to show: a personal one on +2 and a group one on +3. Each can
     * only render once its type has reached the cache too, and nothing offline can fetch either.
     */
    private fun awaitSeedHeld() {
        calendar.awaitDayShows(world.day(2), "CUR")
        calendar.awaitDayShows(world.day(3), "MN")
    }

    /** Moves alice's `personalEvents` marker for [month], the way another device's write would. */
    private fun markPersonalEventsMonth(month: LocalDate) {
        val sync = Documents.get("users/alice/sync/updates")
        val stamped = (sync["personalEvents"] as? JsonObject).orEmpty() +
                (month.yearMonth.toString() to buildJsonObject {
                    put("updatedAt", FirestoreRest.SERVER_TIMESTAMP)
                })
        FirestoreRest.write(
            mapOf(
                "users/alice/sync/updates" to
                        JsonObject(sync + ("personalEvents" to JsonObject(stamped)))
            )
        )
    }

    /** One event filed under both months it spans, with the markers its writer would have moved. */
    private fun seedAlicesSpanningOneOff(start: LocalDate, end: LocalDate) {
        val now = FirestoreRest.SERVER_TIMESTAMP
        val oneOff = buildJsonObject {
            put("name", SPANNING)
            put("color", "#3366FF")
            put("start", "${start}T09:00")
            put("end", "${end}T10:00")
            put("allDay", false)
            put("yearMonthStart", start.yearMonth.toString())
            put("yearMonthEnd", end.yearMonth.toString())
            put("notes", JsonNull)
            put("isDeleted", false)
            put("updateAt", now)
        }
        val sync = Documents.get("users/alice/sync/updates")
        val stamped = (sync["personalOneOffEvents"] as? JsonObject).orEmpty() +
                setOf(start.yearMonth, end.yearMonth).associate {
                    it.toString() to buildJsonObject { put("updatedAt", now) }
                }
        FirestoreRest.write(
            mapOf(
                "users/alice/personalOneOffEvents/$SPANNING_ID" to oneOff,
                "users/alice/sync/updates" to
                        JsonObject(sync + ("personalOneOffEvents" to JsonObject(stamped))),
            )
        )
    }

    private fun serverRead() = runBlocking {
        Network.firestore.collection("users/alice/personalEvents").get(Source.SERVER)
    }

    private companion object {
        /** Anything that would give away that an event has not reached the server yet. */
        val PENDING_WORDING = listOf(
            "Pending", "pending", "Not saved", "not saved", "Unsynced", "Not synced",
            "Retry", "Try again", "Failed", "failed",
        )

        const val SPANNING = "Congress"
        const val SPANNING_ID = "o-alice-spanning"
    }
}
