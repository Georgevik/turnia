package com.geoviksoft.turnia.core.data.user

import com.geoviksoft.turnia.core.domain.analytics.Analytics
import com.geoviksoft.turnia.core.domain.analytics.AnalyticsEvent
import com.geoviksoft.turnia.core.domain.model.EventTypeId
import com.geoviksoft.turnia.core.domain.model.GroupEventType
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.MoveError
import com.geoviksoft.turnia.core.domain.model.MoveResult
import com.geoviksoft.turnia.core.domain.model.MoveScope
import com.geoviksoft.turnia.core.domain.model.PersonalTypedEvent
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.errorOrNull
import com.geoviksoft.turnia.core.system.toFailure
import com.geoviksoft.turnia.core.system.toSuccess
import com.geoviksoft.turnia.core.system.valueOrNull
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.yearMonth

/** Where a move reads the days already taken and commits its chunks: Firestore, or a fake in tests. */
interface PersonalEventMoveStore {

    /** The days the user holds a shift of [groupId] in [months], as the server has them. */
    suspend fun heldDates(uid: UserId, groupId: GroupId, months: Set<YearMonth>): Outcome<Set<LocalDate>, Unit>

    /**
     * One commit: [events] become shifts of [target] and stop being personal. [deleteType], when
     * given, is soft-deleted in the same commit.
     */
    suspend fun commit(
        uid: UserId,
        target: GroupEventType,
        events: List<PersonalTypedEvent>,
        deleteType: EventTypeId?,
    ): Outcome<Unit, Unit>
}

internal data class MovePlan(val chunks: List<List<PersonalTypedEvent>>, val skipped: Int)

/**
 * A day already held is skipped, and so is a second event on a day this move already fills: the
 * group would otherwise get two of the user's shifts on one day.
 */
internal fun planMove(
    events: List<PersonalTypedEvent>,
    held: Set<LocalDate>,
    chunkSize: Int,
): MovePlan {
    val taken = held.toMutableSet()
    val moving = events.sortedBy { it.date }.filter { taken.add(it.date) }
    return MovePlan(moving.chunked(chunkSize), skipped = events.size - moving.size)
}

internal class PersonalEventMove(
    private val store: PersonalEventMoveStore,
    private val analytics: Analytics,
    private val chunkSize: Int = CHUNK_SIZE,
) {

    suspend fun move(
        uid: UserId,
        events: List<PersonalTypedEvent>,
        target: GroupEventType,
        scope: MoveScope,
    ): Outcome<MoveResult, MoveError> {
        if (events.isEmpty()) return MoveError.Failed.toFailure()

        val months = events.map { it.date.yearMonth }.toSet()
        val held = store.heldDates(uid, target.groupId, months).valueOrNull()
            ?: return MoveError.Failed.toFailure()

        val plan = planMove(events, held, chunkSize)
        if (scope == MoveScope.One && plan.chunks.isEmpty()) return MoveError.DayTaken.toFailure()

        val typeToDelete = events.first().type.id.takeIf { scope == MoveScope.All }
        // Every event skipped still settles "move all": the type goes, in a commit of its own.
        val commits = plan.chunks.ifEmpty { if (typeToDelete != null) listOf(emptyList()) else emptyList() }

        commits.forEachIndexed { index, chunk ->
            val last = index == commits.lastIndex
            store.commit(uid, target, chunk, typeToDelete.takeIf { last }).errorOrNull()?.let {
                return MoveError.Failed.toFailure()
            }
        }

        val result = MoveResult(moved = plan.chunks.sumOf { it.size }, skipped = plan.skipped)
        analytics.log(AnalyticsEvent.PersonalEventsMoved(scope, result.moved, result.skipped))
        return result.toSuccess()
    }

    companion object {
        /**
         * Three writes an event at most — the shift, the personal event's delete and its note —
         * plus the markers, well under a batch's 500. Kept here alone, so the rules' document-access
         * budget can shrink it without touching anything else.
         */
        const val CHUNK_SIZE = 150
    }
}
