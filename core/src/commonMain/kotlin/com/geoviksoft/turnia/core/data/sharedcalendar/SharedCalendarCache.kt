package com.geoviksoft.turnia.core.data.sharedcalendar

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.geoviksoft.turnia.core.data.datasource.firestorefunctions.requests.SharedCalendarResponse
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.system.outcomeCatching
import com.geoviksoft.turnia.core.system.valueOrNull
import kotlinx.coroutines.flow.first
import kotlinx.datetime.YearMonth
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A month of a colleague's calendar as the server last described it, and how far it is caught up.
 *
 * [cursor] and [seen] are two clocks on purpose. [cursor] comes from the documents' own `updateAt`,
 * so it is what bounds the next `since`. [seen] holds the owner's sync markers as they read when
 * this was fetched: `groupEvents` is stamped by a trigger after its event commits, so it is always
 * later than any `updateAt`, and comparing it with [cursor] would never settle.
 */
@Serializable
data class CachedSharedCalendar(
    @SerialName("response") val response: SharedCalendarResponse,
    @SerialName("cursor") val cursor: String? = null,
    /** Marker key to epoch milliseconds. A marker the owner has never written is absent. */
    @SerialName("seen") val seen: Map<String, Long> = emptyMap(),
)

/**
 * Colleagues' calendars on the device, one entry per viewer, owner and month, so a month seen
 * before paints without a call — after the app was killed too — and old months outlive the
 * server's retention window.
 *
 * Keyed by the viewer as well: another account signed in on this device must not see it.
 */
class SharedCalendarCache(private val dataStore: DataStore<Preferences>) {

    suspend fun read(viewerId: UserId, ownerId: UserId, month: YearMonth): CachedSharedCalendar? {
        val raw = outcomeCatching(TAG, mapError = {}) {
            dataStore.data.first()[key(viewerId, ownerId, month)]
        }.valueOrNull() ?: return null

        // An entry this build cannot read is a miss: one full fetch, not a crash.
        return outcomeCatching(TAG, mapError = {}) {
            json.decodeFromString<CachedSharedCalendar>(raw)
        }.valueOrNull()
    }

    /**
     * Folds [answer] into what is cached: removals go, changed events replace their old selves, and
     * everything else stays. A document the server purged never shows up in an answer, so a past
     * month keeps its events here.
     */
    suspend fun merge(
        viewerId: UserId,
        ownerId: UserId,
        month: YearMonth,
        answer: SharedCalendarResponse,
        seen: Map<String, Long>,
    ): CachedSharedCalendar {
        val previous = read(viewerId, ownerId, month)
        val merged = CachedSharedCalendar(
            response = previous?.response?.let { merge(it, answer) } ?: answer,
            cursor = listOfNotNull(previous?.cursor, answer.cursor).maxOrNull(),
            seen = seen,
        )

        outcomeCatching(TAG, mapError = {}) {
            dataStore.edit { it[key(viewerId, ownerId, month)] = json.encodeToString(merged) }
        }
        return merged
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

    private fun merge(cached: SharedCalendarResponse, answer: SharedCalendarResponse): SharedCalendarResponse {
        val groupGone = answer.removedGroupEventIds.toSet() +
            answer.groupEvents.map { "${it.groupId}/${it.eventId}" }
        val personalGone = answer.removedPersonalEventIds.toSet() + answer.personalEvents.map { it.eventId }
        val oneOffGone = answer.removedPersonalOneOffEventIds.toSet() +
            answer.personalOneOffEvents.map { it.eventId }

        return answer.copy(
            groupEvents = cached.groupEvents.filterNot { "${it.groupId}/${it.eventId}" in groupGone } +
                answer.groupEvents,
            personalEvents = cached.personalEvents.filterNot { it.eventId in personalGone } +
                answer.personalEvents,
            personalOneOffEvents = cached.personalOneOffEvents.filterNot { it.eventId in oneOffGone } +
                answer.personalOneOffEvents,
            // Names come only for the holders of the shifts in the answer; the cached shifts still
            // need theirs.
            userNames = cached.userNames + answer.userNames,
            removedGroupEventIds = emptyList(),
            removedPersonalEventIds = emptyList(),
            removedPersonalOneOffEventIds = emptyList(),
        )
    }

    private fun key(viewerId: UserId, ownerId: UserId, month: YearMonth) =
        stringPreferencesKey(prefix(viewerId, ownerId) + month.toString())

    private fun prefix(viewerId: UserId, ownerId: UserId) = "${viewerId.value}|${ownerId.value}|"

    private companion object {
        const val TAG = "SharedCalendarCache"
        val json = Json { ignoreUnknownKeys = true }
    }
}
