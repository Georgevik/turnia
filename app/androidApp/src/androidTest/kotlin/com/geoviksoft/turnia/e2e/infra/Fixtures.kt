package com.geoviksoft.turnia.e2e.infra

import androidx.test.platform.app.InstrumentationRegistry
import com.geoviksoft.turnia.e2e.infra.Fixtures.withSyncMarkers
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.plus
import kotlinx.datetime.yearMonth
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.time.Instant

internal data class FixtureUser(val uid: String, val email: String, val password: String)

/** A fixture after its placeholders were filled in: what is actually written to the emulators. */
internal class Fixture(
    val users: Map<String, FixtureUser>,
    val documents: Map<String, JsonObject>,
)

/**
 * Loads the worlds under `firebase/test/fixtures`, which the build packs into the test APK.
 *
 * Dates are written relative to the day the test runs, so a fixture never falls out of the month
 * on screen or out of the retention window:
 *
 * - `"$now"`: a timestamp, the same instant everywhere in one seed.
 * - `"$date(+N)"`: the day N days from today, as `YYYY-MM-DD`.
 * - `"$yearMonth(+N)"`: the month of that same day, as `YYYY-MM`.
 *
 * The sync markers are not written by hand. A fresh install serves nothing from a cache it does
 * not have and fetches only what a marker says moved, so a document with no marker is invisible
 * to the app; [withSyncMarkers] adds them exactly as the real writers would have.
 */
internal object Fixtures {

    fun load(names: List<String>, today: LocalDate, now: Instant): Fixture {
        val users = linkedMapOf<String, FixtureUser>()
        val documents = linkedMapOf<String, JsonObject>()

        names.forEach { name ->
            val raw = Json.parseToJsonElement(readAsset("$name.json")).jsonObject
            raw["auth"]?.jsonArray?.forEach { element ->
                val user = element.jsonObject
                val uid = user.string("uid")
                users[uid] = FixtureUser(uid, user.string("email"), user.string("password"))
            }
            // A later fixture replaces a document whole, and null removes it.
            raw["firestore"]?.jsonObject?.forEach { (path, document) ->
                if (document is JsonNull) {
                    documents.remove(path)
                } else {
                    documents[path] = resolve(document, today, now).jsonObject
                }
            }
        }

        return Fixture(users, withSyncMarkers(documents, now))
    }

    private fun readAsset(fileName: String): String =
        InstrumentationRegistry.getInstrumentation().context.assets.open(fileName)
            .bufferedReader().use { it.readText() }

    private fun resolve(element: JsonElement, today: LocalDate, now: Instant): JsonElement =
        when (element) {
            is JsonObject -> JsonObject(element.mapValues { (_, value) -> resolve(value, today, now) })
            is JsonArray -> JsonArray(element.map { resolve(it, today, now) })
            is JsonPrimitive -> if (element.isString) placeholder(element.content, today, now) ?: element else element
        }

    private fun placeholder(value: String, today: LocalDate, now: Instant): JsonElement? {
        if (value == "\$now") return timestamp(now)
        val match = PLACEHOLDER.matchEntire(value) ?: return null
        val (function, offset) = match.destructured
        val date = today.plus(DatePeriod(days = offset.toInt()))
        return when (function) {
            "date" -> JsonPrimitive(date.toString())
            "yearMonth" -> JsonPrimitive(date.yearMonth.toString())
            else -> error("Unknown fixture placeholder: $value")
        }
    }

    private fun withSyncMarkers(
        documents: Map<String, JsonObject>,
        now: Instant,
    ): Map<String, JsonObject> {
        val marker = timestamp(now)
        val markers = linkedMapOf<String, JsonObject>()

        documents.keys.mapNotNull { GROUP.matchEntire(it)?.groupValues?.get(1) }.forEach { groupId ->
            val months = monthsOf(documents, "groups/$groupId/events/")
            val pending = documents
                .filterKeys { it.startsWith("groups/$groupId/joinRequests/") }
                .filterValues { it.stringOrNull("status") == "pending" }
                .map { (path, request) -> path.substringAfterLast('/') to (request["requestedAt"] ?: marker) }

            markers["groups/$groupId/sync/updates"] = buildJsonObject {
                put("group", marker)
                put("events", JsonObject(months.associate { it.toString() to updatedAt(marker) }))
                put("joinRequests", JsonObject(pending.toMap()))
            }
        }

        documents.keys.mapNotNull { USER.matchEntire(it)?.groupValues?.get(1) }.forEach { uid ->
            val months = monthsOf(documents, "users/$uid/personalEvents/")
            val groups = documents
                .filterKeys { GROUP.matches(it) }
                .filterValues { group -> group.strings("memberUids").contains(uid) }
                .keys.map { it.substringAfter('/') }

            markers["users/$uid/sync/updates"] = buildJsonObject {
                put("personalEvents", JsonObject(months.associate { it.toString() to updatedAt(marker) }))
                listOf(
                    "personalEventTypesUpdatedAt", "revokedGroups", "account", "profile",
                    "joinRequests", "preferences",
                ).forEach { put(it, marker) }
                put("groups", JsonObject(groups.associateWith { marker }))
                put("groupsIndexed", true)
            }
        }

        // A marker a fixture spells out itself wins over the derived one.
        return markers + documents
    }

    private fun monthsOf(documents: Map<String, JsonObject>, prefix: String): Set<YearMonth> =
        documents.filterKeys { it.startsWith(prefix) && !it.removePrefix(prefix).contains('/') }
            .values.mapNotNull { it.stringOrNull("yearMonth") }
            .map(YearMonth::parse)
            .toSet()

    private fun updatedAt(marker: JsonElement) = buildJsonObject { put("updatedAt", marker) }

    fun timestamp(instant: Instant): JsonObject = buildJsonObject { put(FirestoreRest.TIMESTAMP, instant.toString()) }

    private fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.content
    private fun JsonObject.stringOrNull(key: String): String? = get(key)?.jsonPrimitive?.contentOrNull
    private fun JsonObject.strings(key: String): List<String> =
        get(key)?.jsonArray.orEmpty().map { it.jsonPrimitive.content }

    private val PLACEHOLDER = Regex("""\$(date|yearMonth)\(([+-]\d+)\)""")
    private val GROUP = Regex("""groups/([^/]+)""")
    private val USER = Regex("""users/([^/]+)""")
}
