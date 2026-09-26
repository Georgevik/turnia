package com.geoviksoft.turnia.core.data.sharedcalendar

import com.geoviksoft.turnia.core.data.datasource.firestore.doc.UserSyncDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.sync.CONFIRMATION_TIMEOUT
import com.geoviksoft.turnia.core.data.datasource.firestore.sync.Synced
import com.geoviksoft.turnia.core.data.datasource.firestorefunctions.requests.SharedCalendarResponse
import com.geoviksoft.turnia.core.data.sharedcalendar.mappers.SharedCalendarMapper
import com.geoviksoft.turnia.core.domain.model.SharedCalendar
import com.geoviksoft.turnia.core.domain.model.SharedCalendarError
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.domain.repository.SharedCalendarRepository
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.map
import com.geoviksoft.turnia.core.system.toInstantOrNull
import com.geoviksoft.turnia.core.system.toSuccess
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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minus
import kotlinx.datetime.minusMonth
import kotlinx.datetime.plus
import kotlinx.datetime.plusMonth

/**
 * A colleague's calendar, one month at a time, served from the device and caught up only when the
 * owner's sync markers say it is behind — and then only by the documents that changed.
 *
 * The group's own markers are for its members, which the viewer usually is not. What they follow
 * instead is the owner's `sync/updates`, which a grant already lets them read: `groupEvents` there
 * is stamped by the server for every holder of a shift that moved.
 */
class SharedCalendarRepositoryImpl(
    private val fetch: suspend (
        ownerId: UserId,
        from: LocalDate,
        to: LocalDate,
        since: String?,
    ) -> Outcome<SharedCalendarResponse, SharedCalendarError>,
    private val markers: (ownerId: UserId) -> Flow<Synced<UserSyncDocument>>,
    private val viewerId: () -> UserId?,
    private val cache: SharedCalendarCache,
    private val mapper: SharedCalendarMapper,
) : SharedCalendarRepository {

    override fun sharedCalendar(
        ownerId: UserId,
        month: YearMonth,
    ): Flow<Outcome<SharedCalendar, SharedCalendarError>> = channelFlow {
        val from = month.firstDay.minus(NEIGHBOUR_DAYS, DateTimeUnit.DAY)
        val to = month.lastDay.plus(NEIGHBOUR_DAYS, DateTimeUnit.DAY)
        val viewer = viewerId()
        if (viewer == null) {
            send(fetch(ownerId, from, to, null).map(mapper::map))
            return@channelFlow
        }

        cache.read(viewer, ownerId, month)?.let { send(mapper.map(it.response).toSuccess()) }

        val months = listOf(month.minusMonth(), month, month.plusMonth())
        confirmedMarkers(ownerId)
            .map { it.relevantTo(months) }
            .distinctUntilChanged()
            // A marker that moves while a call is out is handled after it, not by cancelling a
            // call that has been billed already.
            .conflate()
            .collect { seen ->
                val cached = cache.read(viewer, ownerId, month)
                if (cached != null && !cached.isBehind(seen)) return@collect

                when (val answer = fetch(ownerId, from, to, cached?.cursor)) {
                    is Outcome.Success -> {
                        val merged = cache.merge(viewer, ownerId, month, answer.value, seen)
                        send(mapper.map(merged.response).toSuccess())
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

    /**
     * Only what the server stands behind: a first snapshot from Firestore's own cache would read as
     * "no markers", and the confirmed one after it as "behind" — two calls for one open. Offline,
     * where nothing is ever confirmed, the cached snapshot is used after a short wait.
     */
    private fun confirmedMarkers(ownerId: UserId): Flow<UserSyncDocument> = flow {
        val source = markers(ownerId)
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
    }.mapNotNull { (key, marker) ->
        marker.toInstantOrNull()?.let { key to it.toEpochMilliseconds() }
    }.toMap()

    /** A marker is only ever compared with an earlier reading of itself, never with an `updateAt`. */
    private fun CachedSharedCalendar.isBehind(markers: Map<String, Long>): Boolean =
        markers.any { (key, marker) -> seen[key]?.let { marker > it } ?: true }

    private companion object {
        /** A month grid shows up to two weeks of each neighbouring month. */
        const val NEIGHBOUR_DAYS = 14
        const val GROUP_EVENTS = "groupEvents"
        const val PERSONAL_EVENTS = "personalEvents"
        const val PERSONAL_ONE_OFF_EVENTS = "personalOneOffEvents"
        const val PERSONAL_EVENT_TYPES = "personalEventTypes"
    }
}
