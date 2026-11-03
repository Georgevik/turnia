package com.geoviksoft.turnia.core.data.user

import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.core.domain.analytics.Analytics
import com.geoviksoft.turnia.core.domain.analytics.AnalyticsEvent
import com.geoviksoft.turnia.core.domain.model.ProfilePhoto
import com.geoviksoft.turnia.core.domain.model.ProfilePhotoError
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.errorOrNull
import com.geoviksoft.turnia.core.system.toFailure
import com.geoviksoft.turnia.core.system.toSuccess
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * A photo goes up in two steps: the file to Storage, then `setProfilePhoto`, which is what makes it
 * the profile's and copies it into every group. Only once both have succeeded is it reported.
 *
 * Takes the two steps, and the log, as functions, so the sequence can be tested without Firebase or
 * Android's `Log`.
 */
@OptIn(ExperimentalUuidApi::class)
class ProfilePhotoPublisher(
    private val upload: suspend (ProfilePhoto, ByteArray) -> Outcome<Unit, Any>,
    private val publish: suspend (photoId: String?) -> Outcome<Unit, Any>,
    private val analytics: Analytics,
    // A new photo is always a new file: a path is never reused, so a cache never goes stale.
    private val newPhotoId: () -> String = { Uuid.random().toString() },
    private val logError: (String) -> Unit = { Logger.e(TAG, it) },
) {

    suspend fun upload(userId: UserId, bytes: ByteArray): Outcome<ProfilePhoto, ProfilePhotoError> {
        val photo = ProfilePhoto(userId, newPhotoId())

        upload(photo, bytes).errorOrNull()?.let { error ->
            logError("Failed to upload the photo: $error")
            return ProfilePhotoError.UploadFailed.toFailure()
        }
        // An upload that is never published is deleted by the next set or remove.
        publish(photo.photoId).errorOrNull()?.let { error ->
            logError("Failed to publish the photo: $error")
            return ProfilePhotoError.UploadFailed.toFailure()
        }

        analytics.log(AnalyticsEvent.ProfilePhotoUploaded)
        return photo.toSuccess()
    }

    suspend fun remove(): Outcome<Unit, ProfilePhotoError> {
        publish(null).errorOrNull()?.let { error ->
            logError("Failed to remove the photo: $error")
            return ProfilePhotoError.RemoveFailed.toFailure()
        }
        return Unit.toSuccess()
    }

    private companion object {
        const val TAG = "ProfilePhotoPublisher"
    }
}
