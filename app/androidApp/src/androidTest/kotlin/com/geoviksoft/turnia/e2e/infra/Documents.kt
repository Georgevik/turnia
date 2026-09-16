package com.geoviksoft.turnia.e2e.infra

import android.os.SystemClock
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * What the emulator holds, checked from outside the app. The app writes asynchronously and the
 * callables run on the host, so every check polls until it holds or [timeoutMs] runs out.
 */
internal object Documents {

    fun await(
        path: String,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        condition: (JsonObject) -> Boolean,
    ): JsonObject = poll("$path to match", timeoutMs) {
        FirestoreRest.get(path)?.takeIf(condition)
    }

    /** The first document under [collectionPath] matching [condition], by id. */
    fun awaitIn(
        collectionPath: String,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        condition: (JsonObject) -> Boolean,
    ): Pair<String, JsonObject> = poll("a document in $collectionPath to match", timeoutMs) {
        FirestoreRest.list(collectionPath).entries.firstOrNull { condition(it.value) }?.toPair()
    }

    fun get(path: String): JsonObject =
        checkNotNull(FirestoreRest.get(path)) { "Expected a document at $path" }

    private fun <T : Any> poll(what: String, timeoutMs: Long, read: () -> T?): T {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (true) {
            read()?.let { return it }
            check(SystemClock.uptimeMillis() < deadline) { "Timed out after $timeoutMs ms waiting for $what" }
            SystemClock.sleep(POLL_MS)
        }
    }

    // Callables cold-start in the Functions emulator, which can take several seconds on CI.
    private const val DEFAULT_TIMEOUT_MS = 30_000L
    private const val POLL_MS = 250L
}

internal fun JsonObject.string(key: String): String? = get(key)?.jsonPrimitive?.contentOrNull
internal fun JsonObject.boolean(key: String): Boolean? = get(key)?.jsonPrimitive?.booleanOrNull
internal fun JsonObject.strings(key: String): List<String> =
    get(key)?.jsonArray.orEmpty().map { it.jsonPrimitive.content }
internal fun JsonObject.objects(key: String): List<JsonObject> =
    get(key)?.jsonArray.orEmpty().map(JsonElement::jsonObject)
