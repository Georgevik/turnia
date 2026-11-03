package com.geoviksoft.turnia.ui.system.photo

import com.geoviksoft.turnia.core.domain.model.ProfilePhoto
import com.geoviksoft.turnia.core.domain.model.UserId
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfilePhotoCacheTest {

    private val directory = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "profile-photo-cache-test-${Random.nextLong()}"
    private val downloads = mutableListOf<ProfilePhoto>()
    private val photo = ProfilePhoto(UserId("bruno"), "p1")
    private val bytes = byteArrayOf(1, 2, 3)

    private fun cache(available: ByteArray? = bytes) = ProfilePhotoCache(directory, download = { photo ->
        downloads += photo
        available
    })

    @AfterTest
    fun cleanUp() = FileSystem.SYSTEM.deleteRecursively(directory)

    @Test
    fun aPhotoIsDownloadedOnceAndThenReadFromDisk() = runTest {
        val cache = cache()

        val first = cache.load(photo)
        val second = cache.load(photo)

        assertTrue(first!!.downloaded)
        assertFalse(second!!.downloaded)
        assertContentEquals(bytes, second.bytes)
        assertEquals(1, downloads.size)
    }

    @Test
    fun aPrimedPhotoIsNeverDownloaded() = runTest {
        val cache = cache()

        cache.prime(photo, bytes)

        assertContentEquals(bytes, cache.load(photo)!!.bytes)
        assertTrue(downloads.isEmpty())
    }

    @Test
    fun anotherPhotoOfTheSameUserIsAnotherFile() = runTest {
        val cache = cache()
        cache.prime(photo, bytes)

        cache.load(photo.copy(photoId = "p2"))

        assertEquals(listOf(photo.copy(photoId = "p2")), downloads)
    }

    @Test
    fun aPhotoThatCannotBeHadIsNull() = runTest {
        assertNull(cache(available = null).load(photo))
    }

    @Test
    fun aPhotoThatCannotBeHadIsNotAskedForAgain() = runTest {
        val cache = cache(available = null)

        cache.load(photo)
        cache.load(photo)

        assertEquals(1, downloads.size)
    }
}
