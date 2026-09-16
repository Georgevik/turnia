package com.geoviksoft.turnia.e2e.infra

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** The Auth emulator's REST API: accounts with the uids the fixtures reference. */
internal object AuthRest {

    private val base get() = "http://${Emulator.HOST}:${Emulator.AUTH_PORT}"

    fun wipe() {
        Http.send("DELETE", "$base/emulator/v1/projects/${Emulator.projectId}/accounts")
            .requireOk("Wiping Auth")
    }

    fun create(user: FixtureUser) {
        val body = buildJsonObject {
            put("localId", user.uid)
            put("email", user.email)
            put("password", user.password)
        }
        Http.send("POST", "$base/identitytoolkit.googleapis.com/v1/projects/${Emulator.projectId}/accounts", body)
            .requireOk("Creating the account ${user.uid}")
    }
}
