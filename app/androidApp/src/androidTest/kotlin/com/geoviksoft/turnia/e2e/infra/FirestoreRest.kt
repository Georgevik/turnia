package com.geoviksoft.turnia.e2e.infra

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * The Firestore emulator's REST API, with documents as plain JSON. A Firestore timestamp is the one
 * type JSON has no word for, so it travels as `{"$timestamp": "<RFC 3339>"}` both ways.
 */
internal object FirestoreRest {

    const val TIMESTAMP = "\$timestamp"

    private val base get() = "http://${Emulator.HOST}:${Emulator.FIRESTORE_PORT}"
    private val database get() = "projects/${Emulator.projectId}/databases/(default)"

    fun wipe() {
        // The emulator answers 499 when the wipe cancels a listener the previous test's process
        // left open; the documents are gone all the same once a retry comes back clean.
        repeat(WIPE_ATTEMPTS - 1) {
            if (Http.send("DELETE", "$base/emulator/v1/$database/documents").ok) return
            Thread.sleep(WIPE_RETRY_MS)
        }
        Http.send("DELETE", "$base/emulator/v1/$database/documents").requireOk("Wiping Firestore")
    }

    fun write(documents: Map<String, JsonObject>) {
        documents.entries.chunked(MAX_WRITES_PER_COMMIT).forEach { chunk ->
            val body = buildJsonObject {
                putJsonArray("writes") {
                    chunk.forEach { (path, fields) ->
                        add(buildJsonObject {
                            putJsonObject("update") {
                                put("name", "$database/documents/$path")
                                put("fields", encodeFields(fields))
                            }
                        })
                    }
                }
            }
            Http.send("POST", "$base/v1/$database/documents:commit", body).requireOk("Seeding Firestore")
        }
    }

    /** The document at [path], or null if there is none. */
    fun get(path: String): JsonObject? {
        val response = Http.send("GET", "$base/v1/$database/documents/$path")
        if (response.code == 404) return null
        val document = Json.parseToJsonElement(response.requireOk("Reading $path").body).jsonObject
        return decodeFields(document["fields"])
    }

    /** Every document directly under [collectionPath], by id. */
    fun list(collectionPath: String): Map<String, JsonObject> {
        val documents = mutableMapOf<String, JsonObject>()
        var pageToken: String? = null
        do {
            val query = "pageSize=300" + (pageToken?.let { "&pageToken=$it" } ?: "")
            val response = Http.send("GET", "$base/v1/$database/documents/$collectionPath?$query")
                .requireOk("Listing $collectionPath")
            val page = Json.parseToJsonElement(response.body).jsonObject
            page["documents"]?.jsonArray?.forEach { element ->
                val document = element.jsonObject
                val id = document.getValue("name").jsonPrimitive.content.substringAfterLast('/')
                documents[id] = decodeFields(document["fields"])
            }
            pageToken = page["nextPageToken"]?.jsonPrimitive?.content
        } while (pageToken != null)
        return documents
    }

    private fun encodeFields(fields: JsonObject): JsonObject = buildJsonObject {
        fields.forEach { (name, value) -> put(name, encode(value)) }
    }

    private fun encode(value: JsonElement): JsonObject = when (value) {
        JsonNull -> buildJsonObject { put("nullValue", JsonNull) }
        is JsonArray -> buildJsonObject {
            putJsonObject("arrayValue") { put("values", JsonArray(value.map(::encode))) }
        }
        is JsonObject -> value[TIMESTAMP]?.let { timestamp ->
            buildJsonObject { put("timestampValue", timestamp) }
        } ?: buildJsonObject {
            putJsonObject("mapValue") { put("fields", encodeFields(value)) }
        }
        is JsonPrimitive -> when {
            value.isString -> buildJsonObject { put("stringValue", value.content) }
            value.booleanOrNull != null -> buildJsonObject { put("booleanValue", value.booleanOrNull) }
            value.content.any { it == '.' || it == 'e' || it == 'E' } ->
                buildJsonObject { put("doubleValue", value) }
            else -> buildJsonObject { put("integerValue", value.content) }
        }
    }

    private fun decodeFields(fields: JsonElement?): JsonObject =
        JsonObject(fields?.jsonObject.orEmpty().mapValues { (_, value) -> decode(value.jsonObject) })

    private fun decode(value: JsonObject): JsonElement {
        val (type, content) = value.entries.single()
        return when (type) {
            "nullValue" -> JsonNull
            "stringValue", "booleanValue", "doubleValue" -> content
            "integerValue" -> JsonPrimitive(content.jsonPrimitive.content.toLong())
            "timestampValue" -> buildJsonObject { put(TIMESTAMP, content) }
            "arrayValue" -> JsonArray(
                content.jsonObject["values"]?.jsonArray.orEmpty().map { decode(it.jsonObject) }
            )
            "mapValue" -> decodeFields(content.jsonObject["fields"])
            else -> error("Firestore value of type $type is not supported by the E2E helpers")
        }
    }

    private const val WIPE_ATTEMPTS = 5
    private const val WIPE_RETRY_MS = 500L

    // Firestore's own limit on a single commit.
    private const val MAX_WRITES_PER_COMMIT = 500
}
