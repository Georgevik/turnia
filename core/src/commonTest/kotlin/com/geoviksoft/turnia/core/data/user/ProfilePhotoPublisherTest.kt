package com.geoviksoft.turnia.core.data.user

import com.geoviksoft.turnia.core.domain.model.ProfilePhoto
import com.geoviksoft.turnia.core.domain.model.ProfilePhotoError
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.fakes.RecordingAnalytics
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.errorOrNull
import com.geoviksoft.turnia.core.system.toFailure
import com.geoviksoft.turnia.core.system.toSuccess
import com.geoviksoft.turnia.core.system.valueOrNull
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfilePhotoPublisherTest {

    private val analytics = RecordingAnalytics()
    private val uploaded = mutableListOf<ProfilePhoto>()
    private val published = mutableListOf<String?>()

    private fun publisher(
        upload: Outcome<Unit, Any> = Unit.toSuccess(),
        publish: Outcome<Unit, Any> = Unit.toSuccess(),
    ) = ProfilePhotoPublisher(
        upload = { photo, _ -> uploaded += photo; upload },
        publish = { photoId -> published += photoId; publish },
        analytics = analytics,
        newPhotoId = { "p1" },
        logError = {},
    )

    @Test
    fun anUploadIsPublishedUnderItsNewIdAndReportedOnce() = runTest {
        val photo = publisher().upload(UserId("alice"), byteArrayOf(1)).valueOrNull()

        assertEquals(ProfilePhoto(UserId("alice"), "p1"), photo)
        assertEquals(listOf<String?>("p1"), published)
        assertEquals(1, analytics.named("profile_photo_uploaded").size)
    }

    @Test
    fun aFailedUploadIsNeitherPublishedNorReported() = runTest {
        val result = publisher(upload = "offline".toFailure()).upload(UserId("alice"), byteArrayOf(1))

        assertEquals(ProfilePhotoError.UploadFailed, result.errorOrNull())
        assertTrue(published.isEmpty())
        assertTrue(analytics.events.isEmpty())
    }

    @Test
    fun aFailedPublishIsNotReported() = runTest {
        val result = publisher(publish = "refused".toFailure()).upload(UserId("alice"), byteArrayOf(1))

        assertEquals(ProfilePhotoError.UploadFailed, result.errorOrNull())
        assertTrue(analytics.events.isEmpty())
    }

    @Test
    fun removingPublishesNoPhotoAndReportsNothing() = runTest {
        publisher().remove()

        assertEquals(listOf<String?>(null), published)
        assertTrue(uploaded.isEmpty())
        assertTrue(analytics.events.isEmpty())
    }

    @Test
    fun aFailedRemoval() = runTest {
        assertEquals(ProfilePhotoError.RemoveFailed, publisher(publish = "refused".toFailure()).remove().errorOrNull())
    }
}
