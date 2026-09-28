package com.geoviksoft.turnia.core.data.sharedcalendar

import com.geoviksoft.turnia.core.data.datasource.firestore.UserSyncFirestore
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.UserSyncDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.sync.CONFIRMATION_TIMEOUT
import com.geoviksoft.turnia.core.data.datasource.firestorefunctions.SharedCalendarFunction
import com.geoviksoft.turnia.core.data.sharedcalendar.mappers.SharedCalendarMapper
import com.geoviksoft.turnia.core.domain.model.RetentionWindow
import com.geoviksoft.turnia.core.domain.model.SharedCalendar
import com.geoviksoft.turnia.core.domain.model.SharedCalendarError
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.domain.repository.SharedCalendarRepository
import com.geoviksoft.turnia.core.domain.repository.UserRepository
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.map
import com.geoviksoft.turnia.core.system.outcomeCatching
import com.geoviksoft.turnia.core.system.toInstantOrNull
import com.geoviksoft.turnia.core.system.toSuccess
import com.geoviksoft.turnia.core.system.valueOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minusMonth
import kotlinx.datetime.plusMonth
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/**
 * A colleague's calendar, one month at a time, served from the device and caught up only when the
 * owner's sync markers say it is behind — and then only by the documents that changed.
 *
 * The group's own markers are for its members, which the viewer usually is not. What they follow
 * instead is the owner's `sync/updates`, which a grant already lets them read: `groupEvents` there
 * is stamped by the server for every holder of a shift that moved.
 */
class SharedCalendarRepositoryImpl(
    private val sharedCalendarFunction: SharedCalendarFunction,
    private val userSyncFirestore: UserSyncFirestore,
    private val userRepository: UserRepository,
    private val cache: SharedCalendarCache,
    private val mapper: SharedCalendarMapper,
) : SharedCalendarRepository {

    override fun sharedCalendar(
        ownerId: UserId,
        month: YearMonth,
    ): Flow<Outcome<SharedCalendar, SharedCalendarError>> = channelFlow {
        // A whole month either side: the grid shows the neighbours' edges, and paging to one finds
        // it already painted while its own answer is on the way. Three months stay within the
        // function's 92-day cap.
        val from = month.minusMonth().firstDay
        val to = month.plusMonth().lastDay
        val viewer = userRepository.loggedUser?.id
        if (viewer == null) {
            send(sharedCalendarFunction.getSharedCalendar(ownerId, from, to, null).map(mapper::map))
            return@channelFlow
        }

        cache.read(viewer, ownerId, month)?.let { cached ->
            mapped(viewer, ownerId, month, cached)?.let { send(it.toSuccess()) }
        }

        val months = listOf(month.minusMonth(), month, month.plusMonth())
        confirmedMarkers(ownerId)
            .map { it?.relevantTo(months) }
            .distinctUntilChanged()
            // A marker that moves while a call is out is handled after it, not by cancelling a
            // call that has been billed already.
            .conflate()
            .collect { seen ->
                val cached = cache.read(viewer, ownerId, month)
                // No markers means the listener failed, most likely because the grant is gone:
                // only a call can tell, and a `NotShared` answer is what clears the cache.
                if (cached != null && seen != null && !cached.isBehind(seen)) return@collect

                val since = cached?.cursor?.takeUnless(::isTooOld)
                when (val answer = sharedCalendarFunction.getSharedCalendar(ownerId, from, to, since)) {
                    is Outcome.Success -> {
                        val updated = cache.update(
                            viewer, ownerId, month, answer.value,
                            since = since,
                            seen = seen.orEmpty(),
                            purgedBefore = purgedBefore(),
                        )
                        mapped(viewer, ownerId, month, updated)?.let { send(it.toSuccess()) }
                    }

                    is Outcome.Failure -> when (answer.error) {
                        SharedCalendarError.NotShared -> {
                            cache.removeOwner(viewer, ownerId)
                            send(answer)
                        }
                        // What is on screen stays: the next marker, or the next visit, tries again.
                        else -> if (cached == null) send(answer)
                    }
                }
            }
    }

    /** An entry this build cannot turn into a calendar is dropped rather than crashing the screen. */
    private suspend fun mapped(
        viewer: UserId,
        ownerId: UserId,
        month: YearMonth,
        entry: CachedSharedCalendar,
    ): SharedCalendar? {
        val calendar = outcomeCatching(TAG, mapError = {}) { mapper.map(entry.response) }.valueOrNull()
        if (calendar == null) cache.remove(viewer, ownerId, month)
        return calendar
    }

    /**
     * Only what the server stands behind: a first snapshot from Firestore's own cache would read as
     * "no markers", and the confirmed one after it as "behind" — two calls for one open. Offline,
     * where nothing is ever confirmed, the cached snapshot is used after a short wait.
     */
    private fun confirmedMarkers(ownerId: UserId): Flow<UserSyncDocument?> = flow {
        val source = userSyncFirestore.observeShared(ownerId)
        val first = withTimeoutOrNull(CONFIRMATION_TIMEOUT) { source.first { it.confirmed } }
            ?: source.first()
        emit(first.value)
        emitAll(source.filter { it.confirmed }.map { it.value })
    }

    private fun UserSyncDocument.relevantTo(months: List<YearMonth>): Map<String, Long> = buildMap {
        months.forEach { month ->
            put("$GROUP_EVENTS/$month", groupEventsUpdatedAt[month]?.updatedAt)
            put("$PERSONAL_EVENTS/$month", personalEventsUpdatedAt[month]?.updatedAt)
            put("$PERSONAL_ONE_OFF_EVENTS/$month", personalOneOffEventsUpdatedAt[month]?.updatedAt)
        }
        put(PERSONAL_EVENT_TYPES, personalEventTypesUpdatedAt)
        // The owner's colour picks are part of how their calendar is painted.
        put(PREFERENCES, preferencesUpdatedAt)
    }.mapNotNull { (key, marker) ->
        marker.toInstantOrNull()?.let { key to it.toEpochMilliseconds() }
    }.toMap()

    /** A marker is only ever compared with an earlier reading of itself, never with an `updateAt`. */
    private fun CachedSharedCalendar.isBehind(markers: Map<String, Long>): Boolean =
        markers.any { (key, marker) -> seen[key]?.let { marker > it } ?: true }

    /**
     * A gap asks every group for all its changes since [cursor], the owner's or not, so after a long
     * absence it can read more than asking for the owner's shifts outright.
     */
    private fun isTooOld(cursor: String): Boolean {
        val instant = outcomeCatching(TAG, mapError = {}) { Instant.parse(cursor) }.valueOrNull()
            ?: return true
        return Clock.System.now() - instant > MAX_GAP_AGE
    }

    /** The server deletes events older than a month; only the device still has those days. */
    private fun purgedBefore() =
        RetentionWindow.start(Clock.System.todayIn(TimeZone.currentSystemDefault()))

    private companion object {
        const val TAG = "SharedCalendarRepository"
        val MAX_GAP_AGE = 1.days
        const val GROUP_EVENTS = "groupEvents"
        const val PERSONAL_EVENTS = "personalEvents"
        const val PERSONAL_ONE_OFF_EVENTS = "personalOneOffEvents"
        const val PERSONAL_EVENT_TYPES = "personalEventTypes"
        const val PREFERENCES = "preferences"
    }
}
