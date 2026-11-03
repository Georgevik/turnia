package com.geoviksoft.turnia.e2e.flows

import android.os.SystemClock
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.geoviksoft.turnia.e2e.infra.AirplaneMode
import com.geoviksoft.turnia.e2e.infra.Documents
import com.geoviksoft.turnia.e2e.infra.E2eRule
import com.geoviksoft.turnia.e2e.infra.RecordingAnalytics
import com.geoviksoft.turnia.e2e.infra.SignedInAs
import com.geoviksoft.turnia.e2e.infra.StorageRest
import com.geoviksoft.turnia.e2e.infra.awaitLogged
import com.geoviksoft.turnia.e2e.infra.awaitNode
import com.geoviksoft.turnia.e2e.infra.string
import com.geoviksoft.turnia.e2e.robots.GroupsRobot
import com.geoviksoft.turnia.e2e.robots.ProfileRobot
import com.geoviksoft.turnia.e2e.robots.SwapRobot
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** A profile photo, from the profile screen to everyone who sees its owner. */
@RunWith(AndroidJUnit4::class)
class ProfilePhotoFlowsTest {

    private val compose = createEmptyComposeRule()

    private val world = E2eRule(compose, signedInAs = "alice", fixtures = listOf("base", "profile-photo"))

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(compose).around(world)

    private val profile = ProfileRobot(compose)
    private val groups = GroupsRobot(compose)
    private val swaps = SwapRobot(compose)

    @Test
    fun uploadingAPhoto_showsItAndPublishesIt() {
        profile.openProfile()

        profile.uploadPhoto()

        profile.awaitPhoto("alice")
        val photoId = checkNotNull(Documents.await("users/alice") { it.string("photoId") != null }.string("photoId"))
        Documents.await("groups/urgencias") { it.member("alice").string("photoId") == photoId }
        assertEquals(listOf("profilePhotos/alice/$photoId.webp"), StorageRest.list("profilePhotos/alice/"))
        val stored = checkNotNull(StorageRest.metadata("profilePhotos/alice/$photoId.webp"))
        assertTrue("${stored.string("size")} bytes", checkNotNull(stored.string("size")).toInt() <= 100 * 1024)
        compose.awaitLogged("profile_photo_uploaded").single()
    }

    @Test
    @SignedInAs("bruno")
    fun removingThePhoto_bringsTheAnimalBack() {
        profile.openProfile()
        profile.awaitPhoto("bruno")

        profile.removePhoto()

        profile.awaitNoPhoto("bruno")
        Documents.await("users/bruno") { it.string("photoId") == null }
        Documents.await("groups/urgencias") { it.member("bruno").string("photoId") == null }
        assertEquals(emptyList<String>(), StorageRest.list("profilePhotos/bruno/"))
        assertTrue("A removal is not reported", RecordingAnalytics.named("profile_photo_uploaded").isEmpty())
    }

    @Test
    fun aColleaguesPhoto_showsOnTheMemberListAndOnTheirSwap() {
        groups.openGroupFromSettings("Urgencias")
        groups.click("3 members")
        profile.awaitPhoto("bruno")

        groups.back()
        groups.back()
        swaps.openColleagues()
        profile.awaitPhoto("bruno")
    }

    /** This build merges an admin's save, so the trigger that heals older builds has nothing to do. */
    @Test
    fun anAdminRenamingTheGroup_keepsThePhotos() {
        groups.openGroupFromSettings("Urgencias")

        profile.renameGroup("Urgencias Norte")

        val renamed = Documents.await("groups/urgencias") { it.string("name") == "Urgencias Norte" }
        assertEquals("bruno1", renamed.member("bruno").string("photoId"))
        SystemClock.sleep(SETTLE_MS)
        assertEquals("No restore was needed", renamed["updateAt"], Documents.get("groups/urgencias")["updateAt"])
    }

    @Test
    fun anUploadOffline_changesNothing() {
        profile.openProfile()

        AirplaneMode.on {
            profile.uploadPhoto()
            compose.awaitNode(hasText(UPLOAD_FAILED), useUnmergedTree = true, timeoutMs = OFFLINE_TIMEOUT_MS)
        }

        profile.awaitNoPhoto("alice")
        assertNull(Documents.get("users/alice").string("photoId"))
        assertTrue(RecordingAnalytics.named("profile_photo_uploaded").isEmpty())
    }

    private fun JsonObject.member(uid: String): JsonObject = getValue("members").jsonObject.getValue(uid).jsonObject

    private companion object {
        const val UPLOAD_FAILED = "We couldn't upload your photo. Please try again."
        // The upload gives up after 30 s; the message follows it.
        const val OFFLINE_TIMEOUT_MS = 90_000L
        const val SETTLE_MS = 4_000L
    }
}
