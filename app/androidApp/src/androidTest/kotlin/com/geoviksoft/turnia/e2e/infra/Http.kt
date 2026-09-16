package com.geoviksoft.turnia.e2e.infra

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.net.HttpURLConnection
import java.net.URL

internal class HttpResponse(val code: Int, val body: String) {
    val ok: Boolean get() = code in 200..299

    fun requireOk(what: String): HttpResponse = also {
        check(ok) { "$what failed with HTTP $code: $body" }
    }
}

/**
 * Plain HTTP to the emulators. `Bearer owner` is the emulators' admin credential: it skips the
 * security rules, which is what seeding and checking the world from outside the app needs.
 */
internal object Http {
    fun send(method: String, url: String, body: JsonElement? = null): HttpResponse {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = method
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("Authorization", "Bearer owner")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(Json.encodeToString(body).toByteArray()) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            HttpResponse(code, stream?.bufferedReader()?.use { it.readText() }.orEmpty())
        } finally {
            connection.disconnect()
        }
    }

    private const val TIMEOUT_MS = 30_000
}
