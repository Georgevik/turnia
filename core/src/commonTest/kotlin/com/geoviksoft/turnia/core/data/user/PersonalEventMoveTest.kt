package com.geoviksoft.turnia.core.data.user

import com.geoviksoft.turnia.core.domain.analytics.AnalyticsEvent
import com.geoviksoft.turnia.core.domain.model.EventId
import com.geoviksoft.turnia.core.domain.model.EventTypeId
import com.geoviksoft.turnia.core.domain.model.GroupEventType
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.MoveError
import com.geoviksoft.turnia.core.domain.model.MoveResult
import com.geoviksoft.turnia.core.domain.model.MoveScope
import com.geoviksoft.turnia.core.domain.model.PersonalEventType
import com.geoviksoft.turnia.core.domain.model.PersonalTypedEvent
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.fakes.RecordingAnalytics
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.toFailure
import com.geoviksoft.turnia.core.system.toSuccess
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PersonalEventMoveTest {

    private val analytics = RecordingAnalytics()
    private val store = FakeStore()
    private val move = PersonalEventMove(store, analytics, chunkSize = 2)

    @Test
    fun aSingleEventBecomesAShiftAndKeepsTheType() = runTest {
        val outcome = move.move(uid, listOf(event(10)), morning, MoveScope.One)

        assertEquals(MoveResult(moved = 1, skipped = 0).toSuccess(), outcome)
        assertEquals(listOf("e10"), store.moved)
        assertNull(store.deletedType)
        assertEquals(1, analytics.named("personal_events_moved").size)
    }

    @Test
    fun allSkipsTakenDaysAndDeletesTheTypeLast() = runTest {
        store.held = setOf(day(3), day(17))

        val outcome = move.move(uid, listOf(3, 10, 17, 24, 25).map(::event), morning, MoveScope.All)

        assertEquals(MoveResult(moved = 3, skipped = 2).toSuccess(), outcome)
        assertEquals(listOf("e10", "e24", "e25"), store.moved)
        // Two chunks of at most two: the type goes with the last one only.
        assertEquals(listOf(null, shift.id), store.commits.map { it.second })
        val logged = analytics.events.single() as AnalyticsEvent.PersonalEventsMoved
        assertEquals(mapOf<String, Any>("scope" to "all", "event_count" to 3L, "skipped_count" to 2L), logged.parameters)
    }

    @Test
    fun twoEventsOnOneDayMoveOnlyOne() = runTest {
        val outcome = move.move(uid, listOf(event(10), event(10, id = "twin")), morning, MoveScope.All)

        assertEquals(MoveResult(moved = 1, skipped = 1).toSuccess(), outcome)
    }

    @Test
    fun oneEventOntoATakenDayWritesNothing() = runTest {
        store.held = setOf(day(10))

        val outcome = move.move(uid, listOf(event(10)), morning, MoveScope.One)

        assertEquals(MoveError.DayTaken.toFailure(), outcome)
        assertTrue(store.commits.isEmpty())
        assertTrue(analytics.events.isEmpty())
    }

    @Test
    fun allSkippedStillDeletesTheType() = runTest {
        store.held = setOf(day(10))

        val outcome = move.move(uid, listOf(event(10)), morning, MoveScope.All)

        assertEquals(MoveResult(moved = 0, skipped = 1).toSuccess(), outcome)
        assertEquals(shift.id, store.deletedType)
    }

    @Test
    fun aFailurePartWayKeepsTheTypeAndARetryFinishesIt() = runTest {
        val events = listOf(1, 2, 3, 4).map(::event)
        store.failOnCommit = 2

        assertEquals(MoveError.Failed.toFailure(), move.move(uid, events, morning, MoveScope.All))
        assertEquals(listOf("e1", "e2"), store.moved)
        assertNull(store.deletedType)
        assertTrue(analytics.events.isEmpty())

        // Run again with what is still personal, as moveCandidates would return it.
        store.failOnCommit = null
        val remaining = events.filterNot { it.id.value in store.moved }
        val retry = move.move(uid, remaining, morning, MoveScope.All)

        assertEquals(MoveResult(moved = 2, skipped = 0).toSuccess(), retry)
        assertEquals(listOf("e1", "e2", "e3", "e4"), store.moved)
        assertEquals(store.moved.toSet().size, store.moved.size)
        assertEquals(shift.id, store.deletedType)
        assertEquals(1, analytics.named("personal_events_moved").size)
    }

    @Test
    fun anUnreadableGroupFails() = runTest {
        store.heldFails = true

        assertEquals(MoveError.Failed.toFailure(), move.move(uid, listOf(event(10)), morning, MoveScope.One))
        assertTrue(store.commits.isEmpty())
    }

    @Test
    fun planOrdersByDateAndChunks() {
        val plan = planMove(listOf(5, 1, 3).map(::event), held = emptySet(), chunkSize = 2)

        assertEquals(listOf(listOf("e1", "e3"), listOf("e5")), plan.chunks.map { c -> c.map { it.id.value } })
        assertEquals(0, plan.skipped)
    }

    private class FakeStore : PersonalEventMoveStore {
        var held = emptySet<LocalDate>()
        var heldFails = false
        var failOnCommit: Int? = null
        val commits = mutableListOf<Pair<List<String>, EventTypeId?>>()
        val moved = mutableListOf<String>()
        var deletedType: EventTypeId? = null

        override suspend fun heldDates(
            uid: UserId,
            groupId: GroupId,
            months: Set<YearMonth>,
        ): Outcome<Set<LocalDate>, Unit> = if (heldFails) Unit.toFailure() else held.toSuccess()

        override suspend fun commit(
            uid: UserId,
            target: GroupEventType,
            events: List<PersonalTypedEvent>,
            deleteType: EventTypeId?,
        ): Outcome<Unit, Unit> {
            if (failOnCommit == commits.size + 1) return Unit.toFailure()
            commits += events.map { it.id.value } to deleteType
            moved += events.map { it.id.value }
            deleteType?.let { deletedType = it }
            return Unit.toSuccess()
        }
    }

    private companion object {
        val uid = UserId("alice")
        val shift = PersonalEventType(EventTypeId("manana"), "Mañana", "#039BE5", "M", null, "08:00", "15:00")
        val morning = GroupEventType(
            id = EventTypeId("morning"),
            groupId = GroupId("urgencias"),
            groupName = "Urgencias",
            name = "Morning",
            acronym = "MN",
            description = null,
            startTime = "08:00",
            endTime = "15:00",
            swappable = true,
            defaultColor = "#039BE5",
            userColor = null,
        )

        fun day(d: Int) = LocalDate(2026, 10, d)
        fun event(d: Int, id: String = "e$d") = PersonalTypedEvent(EventId(id), shift, day(d), notes = null)
    }
}
