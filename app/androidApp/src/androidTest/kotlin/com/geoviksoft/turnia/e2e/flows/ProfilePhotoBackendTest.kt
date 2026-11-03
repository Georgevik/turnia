package com.geoviksoft.turnia.e2e.flows

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.geoviksoft.turnia.e2e.infra.Documents
import com.geoviksoft.turnia.e2e.infra.E2eRule
import com.geoviksoft.turnia.e2e.infra.Emulator
import com.geoviksoft.turnia.e2e.infra.Fixtures
import com.geoviksoft.turnia.e2e.infra.FirestoreRest
import com.geoviksoft.turnia.e2e.infra.SignedInAs
import com.geoviksoft.turnia.e2e.infra.StorageRest
import com.geoviksoft.turnia.e2e.infra.string
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.functions.functions
import dev.gitlive.firebase.storage.Data
import dev.gitlive.firebase.storage.storage
import dev.gitlive.firebase.storage.storageMetadata
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.URL

/**
 * The backend behind a profile photo, driven through the SDK the way the app calls it: the Storage
 * rules, `setProfilePhoto`, and every other writer that has to keep a member's `photoId`.
 */
@RunWith(AndroidJUnit4::class)
class ProfilePhotoBackendTest {

    private val compose = createEmptyComposeRule()

    private val world = E2eRule(compose, signedInAs = "alice", fixtures = listOf("base", "profile-photo"))

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(compose).around(world)

    private val photo by lazy { Fixtures.readAssetBytes("photo-bruno.webp") }

    @Test
    fun setProfilePhoto_publishesToTheProfileAndEveryGroup() = runBlocking {
        upload("profilePhotos/alice/p1.webp", photo)
        upload("profilePhotos/alice/orphan.webp", photo)

        setProfilePhoto("p1")

        val profile = Documents.await("users/alice") { it.string("photoId") == "p1" }
        assertEquals("p1", Documents.get("groups/urgencias").member("alice").string("photoId"))
        assertEquals(
            "The reservation's marker moves with the profile",
            profile["updateAt"], Documents.get("usernames/alice")["updateAt"],
        )
        assertEquals(listOf("profilePhotos/alice/p1.webp"), StorageRest.list("profilePhotos/alice/"))
    }

    @Test
    @SignedInAs("bruno")
    fun setProfilePhotoNull_removesItEverywhere() = runBlocking {
        setProfilePhoto(null)

        Documents.await("users/bruno") { it.string("photoId") == null }
        assertNull(Documents.get("groups/urgencias").member("bruno").string("photoId"))
        assertNull(Documents.get("groups/planta").member("bruno").string("photoId"))
        assertEquals(emptyList<String>(), StorageRest.list("profilePhotos/bruno/"))
    }

    @Test
    fun storageRules_refuseWhatTheAppNeverSends() = runBlocking {
        assertTrue("Over 100 KB", fails { upload("profilePhotos/alice/big.webp", ByteArray(150 * 1024)) })
        assertTrue("Not WebP", fails { upload("profilePhotos/alice/p.webp", photo, contentType = "image/jpeg") })
        assertTrue("Someone else's folder", fails { upload("profilePhotos/bruno/p.webp", photo) })
        assertTrue("An existing photo is never overwritten", fails { upload("profilePhotos/bruno/bruno1.webp", photo) })

        assertEquals(photo.size, Firebase.storage.reference("profilePhotos/bruno/bruno1.webp").getData(MAX_BYTES).data.size)
        assertEquals("No session, no photo", 403, anonymousRead("profilePhotos/bruno/bruno1.webp"))
    }

    @Test
    fun theClient_cannotWritePhotoId() = runBlocking {
        val written = runCatching {
            Firebase.firestore.collection("users").document("alice").updateFields { "photoId" to "p1" }
        }

        assertTrue("The rules freeze photoId", written.isFailure)
        assertNull(Documents.get("users/alice").string("photoId"))
    }

    @Test
    @SignedInAs("bruno")
    fun aRename_keepsThePhoto() = runBlocking {
        callable("updateProfile", mapOf("name" to "Bruno Nuevo", "username" to "bruno"))

        val urgencias = Documents.await("groups/urgencias") { it.member("bruno").string("name") == "Bruno Nuevo" }
        assertEquals("bruno1", urgencias.member("bruno").string("photoId"))
    }

    @Test
    fun acceptingAJoinRequest_copiesThePhoto() = runBlocking {
        val irene = Documents.get("users/irene")
        FirestoreRest.write(mapOf("users/irene" to JsonObject(irene + ("photoId" to JsonPrimitive("irene1")))))
        StorageRest.upload("profilePhotos/irene/irene1.webp", photo)

        callable("acceptJoinRequest", mapOf("groupId" to "urgencias", "uid" to "irene"))

        val urgencias = Documents.await("groups/urgencias") { "irene" in it["members"]!!.jsonObject }
        assertEquals("irene1", urgencias.member("irene").string("photoId"))
    }

    @Test
    @SignedInAs("bruno")
    fun deletingTheAccount_deletesThePhotos() = runBlocking {
        upload("profilePhotos/bruno/unpublished.webp", photo)

        callable("deleteAccount")

        Documents.await("users/bruno") { it.string("photoId") == null && it.string("name") == "" }
        assertNull(Documents.get("groups/urgencias").member("bruno").string("photoId"))
        assertEquals(emptyList<String>(), StorageRest.list("profilePhotos/bruno/"))
    }

    /** What an admin on a build from before photos writes when they save: every entry without `photoId`. */
    @Test
    fun aReleasedClientsGroupSave_getsThePhotoBack() {
        FirestoreRest.write(mapOf("groups/urgencias" to Documents.get("groups/urgencias").withMember("bruno") { JsonObject(it - "photoId") }))

        val healed = Documents.await("groups/urgencias") { it.member("bruno").string("photoId") == "bruno1" }

        SystemClock.sleep(SETTLE_MS)
        assertEquals("The restore causes no second write", healed["updateAt"], Documents.get("groups/urgencias")["updateAt"])
    }

    @Test
    fun aRestoreSkipsWhoeverLeftAndAnonymizedAccounts() {
        FirestoreRest.write(mapOf("users/carla" to JsonObject(Documents.get("users/carla") + ("photoId" to JsonPrimitive("carla1")))))
        val group = Documents.get("groups/urgencias").withMember("carla") { JsonObject(it + ("photoId" to JsonPrimitive("carla1"))) }
        FirestoreRest.write(mapOf("groups/urgencias" to group))
        SystemClock.sleep(SETTLE_MS)

        // Bruno left, keeping a stale entry; carla's account was anonymized.
        val after = JsonObject(
            group.withMember("bruno") { JsonObject(it - "photoId") }
                .withMember("carla") { JsonObject(mapOf("name" to JsonPrimitive(""), "username" to JsonPrimitive(""))) }
                + ("memberUids" to JsonArray(listOf(JsonPrimitive("alice"), JsonPrimitive("carla"))))
        )
        FirestoreRest.write(mapOf("groups/urgencias" to after))
        SystemClock.sleep(SETTLE_MS)

        val urgencias = Documents.get("groups/urgencias")
        assertNull(urgencias.member("bruno").string("photoId"))
        assertNull(urgencias.member("carla").string("photoId"))
    }

    private suspend fun upload(path: String, bytes: ByteArray, contentType: String = StorageRest.WEBP) {
        Firebase.storage.reference(path).putData(Data(bytes), storageMetadata { this.contentType = contentType })
    }

    private suspend fun setProfilePhoto(photoId: String?) {
        callable("setProfilePhoto", mapOf("photoId" to photoId))
    }

    // A map rather than a @Serializable class: this module has no serialization plugin.
    private suspend fun callable(name: String, data: Map<String, String?>) {
        Firebase.functions(Emulator.FUNCTIONS_REGION).httpsCallable(name).invoke(data)
    }

    private suspend fun callable(name: String) {
        Firebase.functions(Emulator.FUNCTIONS_REGION).httpsCallable(name).invoke()
    }

    private suspend fun fails(block: suspend () -> Unit): Boolean = runCatching { block() }.isFailure

    /** A download with no session at all, as a link pasted into a browser would be. */
    private fun anonymousRead(path: String): Int {
        val encoded = java.net.URLEncoder.encode(path, "UTF-8")
        val url = "http://${Emulator.HOST}:${Emulator.STORAGE_PORT}/v0/b/${Emulator.bucket}/o/$encoded?alt=media"
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.responseCode
        } finally {
            connection.disconnect()
        }
    }

    private fun JsonObject.member(uid: String): JsonObject = getValue("members").jsonObject.getValue(uid).jsonObject

    private fun JsonObject.withMember(uid: String, change: (JsonObject) -> JsonObject): JsonObject {
        val members = getValue("members").jsonObject
        return JsonObject(this + ("members" to JsonObject(members + (uid to change(members.getValue(uid).jsonObject)))))
    }

    private companion object {
        const val MAX_BYTES = 100L * 1024
        // Long enough for the Functions emulator to have run any trigger a write set off.
        const val SETTLE_MS = 4_000L
    }
}
