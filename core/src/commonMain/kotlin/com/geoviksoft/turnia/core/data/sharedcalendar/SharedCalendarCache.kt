package com.geoviksoft.turnia.core.data.sharedcalendar

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.geoviksoft.turnia.core.data.datasource.firestorefunctions.requests.SharedCalendarResponse
import com.geoviksoft.turnia.core.data.datasource.firestorefunctions.requests.SharedGroupEventTypeResponse
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.system.outcomeCatching
import com.geoviksoft.turnia.core.system.valueOrNull
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A month of a colleague's calendar as the server last described it, and how far it is caught up.
 *
 * [cursor] and [seen] are two clocks on purpose. [cursor] is the server's own read time, so it is
 * what bounds the next `since`. [seen] holds the owner's sync markers as they read when this was
 * fetched: `groupEvents` is stamped by a trigger after its event commits, so it is always later
 * than any `updateAt`, and comparing it with [cursor] would never settle.
 */
@Serializable
data class CachedSharedCalendar(
    @SerialName("response") val response: SharedCalendarResponse,
    @SerialName("cursor") val cursor: String? = null,
    /** Marker key to epoch milliseconds. A marker the owner has never written is absent. */
    @SerialName("seen") val seen: Map<String, Long> = emptyMap(),
    /** [SharedCalendarCache.VERSION] when it was written; anything else reads as a miss. */
    @SerialName("version") val version: Int = 0,
)

/**
 * Colleagues' calendars on the device, one entry per viewer, owner and month, so a month seen
 * before paints without a call — after the app was killed too — and old months outlive the
 * server's retention window.
 *
 * Keyed by the viewer as well: another account signed in on this device must not see it.
 *
 * A build never trusts an entry it did not write the way it reads: an entry of another [VERSION],
 * or one that does not decode, is a miss and costs one full fetch. The repository also drops one
 * the mapper cannot turn into a calendar, so a stale entry is never a crash.
 */
class SharedCalendarCache(private val dataStore: DataStore<Preferences>) {

    suspend fun read(viewerId: UserId, ownerId: UserId, month: YearMonth): CachedSharedCalendar? =
        outcomeCatching(TAG, mapError = {}) {
            dataStore.data.first()[key(viewerId, ownerId, month)]
        }.valueOrNull()?.let(::decode)

    /**
     * Folds [answer] into what is cached, in one edit so two writers of the same month cannot lose
     * each other's update.
     *
     * A gap ([since] set) is merged: its removals go, changed events replace their old selves, and
     * everything else stays. A whole answer replaces the month, except for the days before
     * [purgedBefore]: the server has deleted those, and this is the only copy left.
     */
    suspend fun update(
        viewerId: UserId,
        ownerId: UserId,
        month: YearMonth,
        answer: SharedCalendarResponse,
        since: String?,
        seen: Map<String, Long>,
        purgedBefore: LocalDate,
    ): CachedSharedCalendar {
        val key = key(viewerId, ownerId, month)
        var updated = CachedSharedCalendar(answer, answer.cursor, seen, VERSION)

        outcomeCatching(TAG, mapError = {}) {
            dataStore.edit { preferences ->
                val previous = preferences[key]?.let(::decode)
                updated = CachedSharedCalendar(
                    response = when {
                        previous == null -> answer
                        since != null -> merge(previous.response, answer)
                        else -> replace(previous.response, answer, purgedBefore)
                    },
                    cursor = answer.cursor ?: previous?.cursor,
                    seen = seen,
                    version = VERSION,
                )
                preferences[key] = json.encodeToString(updated)
            }
        }
        return updated
    }

    suspend fun remove(viewerId: UserId, ownerId: UserId, month: YearMonth) {
        outcomeCatching(TAG, mapError = {}) {
            dataStore.edit { it.remove(key(viewerId, ownerId, month)) }
        }
    }

    /** The owner withdrew the grant: nothing of theirs may stay on this viewer's device. */
    suspend fun removeOwner(viewerId: UserId, ownerId: UserId) {
        val prefix = prefix(viewerId, ownerId)
        outcomeCatching(TAG, mapError = {}) {
            dataStore.edit { preferences ->
                preferences.asMap().keys
                    .filter { it.name.startsWith(prefix) }
                    .forEach { preferences.remove(it) }
            }
        }
    }

    private fun decode(raw: String): CachedSharedCalendar? =
        outcomeCatching(TAG, mapError = {}) { json.decodeFromString<CachedSharedCalendar>(raw) }
            .valueOrNull()
            ?.takeIf { it.version == VERSION }

    private fun merge(cached: SharedCalendarResponse, answer: SharedCalendarResponse): SharedCalendarResponse {
        val groupGone = answer.removedGroupEventIds.toSet() +
            answer.groupEvents.map { "${it.groupId}/${it.eventId}" }
        val personalGone = answer.removedPersonalEventIds.toSet() + answer.personalEvents.map { it.eventId }
        val oneOffGone = answer.removedPersonalOneOffEventIds.toSet() +
            answer.personalOneOffEvents.map { it.eventId }

        return answer.withLookupsOf(cached).copy(
            groupEvents = cached.groupEvents.filterNot { "${it.groupId}/${it.eventId}" in groupGone } +
                answer.groupEvents,
            personalEvents = cached.personalEvents.filterNot { it.eventId in personalGone } +
                answer.personalEvents,
            personalOneOffEvents = cached.personalOneOffEvents.filterNot { it.eventId in oneOffGone } +
                answer.personalOneOffEvents,
        )
    }

    private fun replace(
        cached: SharedCalendarResponse,
        answer: SharedCalendarResponse,
        purgedBefore: LocalDate,
    ): SharedCalendarResponse {
        val cutoff = purgedBefore.toString()
        val groupIds = answer.groupEvents.map { "${it.groupId}/${it.eventId}" }.toSet()
        val personalIds = answer.personalEvents.map { it.eventId }.toSet()
        val oneOffIds = answer.personalOneOffEvents.map { it.eventId }.toSet()

        // Dates compare as text: `YYYY-MM-DD` sorts by day, and a legacy full instant starts with one.
        return answer.withLookupsOf(cached).copy(
            groupEvents = answer.groupEvents + cached.groupEvents.filter {
                it.date.take(DAY_LENGTH) < cutoff && "${it.groupId}/${it.eventId}" !in groupIds
            },
            personalEvents = answer.personalEvents + cached.personalEvents.filter {
                it.date.take(DAY_LENGTH) < cutoff && it.eventId !in personalIds
            },
            personalOneOffEvents = answer.personalOneOffEvents + cached.personalOneOffEvents.filter {
                it.end.take(DAY_LENGTH) < cutoff && it.eventId !in oneOffIds
            },
        )
    }

    /**
     * The answer's lookups, with what only the cache still knows: names come only for the holders
     * of the shifts in the answer, and a group the owner was removed from sends only the types of
     * the shifts the server still has.
     */
    private fun SharedCalendarResponse.withLookupsOf(cached: SharedCalendarResponse): SharedCalendarResponse {
        val revokedTypes = revokedGroupIds.associateWith { groupId ->
            val fresh = groupEventTypes[groupId].orEmpty()
            val freshIds = fresh.map(SharedGroupEventTypeResponse::id).toSet()
            fresh + cached.groupEventTypes[groupId].orEmpty().filterNot { it.id in freshIds }
        }
        return copy(
            userNames = cached.userNames + userNames,
            groupEventTypes = groupEventTypes + revokedTypes,
            removedGroupEventIds = emptyList(),
            removedPersonalEventIds = emptyList(),
            removedPersonalOneOffEventIds = emptyList(),
        )
    }

    private fun key(viewerId: UserId, ownerId: UserId, month: YearMonth) =
        stringPreferencesKey(prefix(viewerId, ownerId) + month.toString())

    private fun prefix(viewerId: UserId, ownerId: UserId) = "${viewerId.value}|${ownerId.value}|"

    companion object {
        /**
         * Bumped whenever what is stored changes shape or meaning, so entries written by an older
         * build are fetched again instead of being read with the wrong assumptions.
         */
        const val VERSION = 1

        private const val TAG = "SharedCalendarCache"
        private const val DAY_LENGTH = 10
        private val json = Json { ignoreUnknownKeys = true }
    }
}
