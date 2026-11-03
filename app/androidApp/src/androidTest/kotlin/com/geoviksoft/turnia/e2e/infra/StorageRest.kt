package com.geoviksoft.turnia.e2e.infra

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder

/**
 * The Storage emulator's REST API, as the owner: it skips `storage.rules`, which is what seeding and
 * checking the bucket from outside the app needs. Only `profilePhotos/` is ever written, so that is
 * the only prefix a wipe has to clear.
 */
internal object StorageRest {

    private val base get() = "http://${Emulator.HOST}:${Emulator.STORAGE_PORT}/v0/b/${Emulator.bucket}/o"

    fun wipe() {
        list(PROFILE_PHOTOS).forEach { path ->
            Http.send("DELETE", "$base/${encode(path)}").requireOk("Deleting $path")
        }
    }

    fun upload(path: String, bytes: ByteArray, contentType: String = WEBP) {
        Http.sendBytes("POST", "$base?name=${encode(path)}", bytes, contentType)
            .requireOk("Uploading $path")
    }

    /** Every object under [prefix], however deep. */
    fun list(prefix: String): List<String> {
        val response = Http.send("GET", "$base?prefix=${encode(prefix)}").requireOk("Listing $prefix")
        val listing = Json.parseToJsonElement(response.body).jsonObject
        val items = listing["items"]?.jsonArray.orEmpty().map { it.jsonObject.getValue("name").jsonPrimitive.content }
        val nested = listing["prefixes"]?.jsonArray.orEmpty().flatMap { list(it.jsonPrimitive.content) }
        return items + nested
    }

    /** The object's metadata (`size`, `contentType`…), or null when there is no such object. */
    fun metadata(path: String): JsonObject? {
        val response = Http.send("GET", "$base/${encode(path)}")
        return if (response.ok) Json.parseToJsonElement(response.body).jsonObject else null
    }

    private fun encode(path: String): String = URLEncoder.encode(path, "UTF-8").replace("+", "%20")

    const val PROFILE_PHOTOS = "profilePhotos/"
    const val WEBP = "image/webp"
}
