package com.geoviksoft.turnia.e2e.flows

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.geoviksoft.turnia.core.data.datasource.storage.ProfilePhotoStorage
import com.geoviksoft.turnia.core.domain.model.ProfilePhoto
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.system.isFailure
import com.geoviksoft.turnia.core.system.isSuccess
import com.geoviksoft.turnia.core.system.valueOrNull
import com.geoviksoft.turnia.e2e.infra.E2eRule
import com.geoviksoft.turnia.e2e.infra.Fixtures
import com.geoviksoft.turnia.e2e.infra.StorageRest
import com.geoviksoft.turnia.e2e.infra.string
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/** The app's own [ProfilePhotoStorage], against the Storage emulator and its rules. */
@RunWith(AndroidJUnit4::class)
class ProfilePhotoStorageTest {

    private val compose = createEmptyComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(compose).around(E2eRule(compose, signedInAs = "alice"))

    private val storage: ProfilePhotoStorage get() = GlobalContext.get().get()

    private val photo by lazy { Fixtures.readAssetBytes("photo-bruno.webp") }

    @Test
    fun anUploadDownloadsTheSameBytes() = runBlocking {
        val stored = ProfilePhoto(UserId("alice"), "s1")

        assertTrue(storage.upload(stored, photo).isSuccess)

        assertArrayEquals(photo, storage.download(stored).valueOrNull())
        val metadata = checkNotNull(StorageRest.metadata(stored.path))
        assertEquals("image/webp", metadata.string("contentType"))
        assertEquals("public, max-age=31536000, immutable", metadata.string("cacheControl"))
    }

    @Test
    fun anOversizedUploadFails() = runBlocking {
        val upload = storage.upload(ProfilePhoto(UserId("alice"), "big"), ByteArray(150 * 1024))

        assertTrue(upload.isFailure)
    }
}
