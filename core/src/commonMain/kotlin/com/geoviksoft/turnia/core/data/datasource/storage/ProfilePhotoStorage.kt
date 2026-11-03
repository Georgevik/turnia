package com.geoviksoft.turnia.core.data.datasource.storage

import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.StorageTransfer
import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackStorage
import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.core.domain.model.ProfilePhoto
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.outcomeCatching
import dev.gitlive.firebase.storage.FirebaseStorage
import dev.gitlive.firebase.storage.storageMetadata
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

data class StorageError(val error: Throwable)

/**
 * Profile photos in Cloud Storage, through the SDK both ways: a download goes through the security
 * rules and App Check like any other request, which a public download URL would skip.
 */
class ProfilePhotoStorage(private val storage: FirebaseStorage) {

    init {
        // Left alone, the SDK keeps retrying an upload for ten minutes, which offline is ten
        // minutes of spinner; the timeout below is what the user actually waits.
        storage.setMaxUploadRetryTime(UPLOAD_TIMEOUT)
    }

    suspend fun upload(photo: ProfilePhoto, bytes: ByteArray): Outcome<Unit, StorageError> =
        outcomeCatching(TAG, ::StorageError) {
            Logger.i(TAG, "Upload profile photo")
            val metadata = storageMetadata {
                contentType = CONTENT_TYPE
                cacheControl = CACHE_CONTROL
            }
            withTimeoutOrNull(UPLOAD_TIMEOUT) {
                storage.reference(photo.path).putData(bytes.toStorageData(), metadata)
            } ?: throw UploadTimeoutException()
            trackStorage(TAG, "upload", StorageTransfer.UPLOAD, bytes.size)
        }

    suspend fun download(photo: ProfilePhoto): Outcome<ByteArray, StorageError> =
        outcomeCatching(TAG, ::StorageError) {
            val bytes = storage.reference(photo.path).getData(MAX_BYTES).toByteArray()
            trackStorage(TAG, "download", StorageTransfer.DOWNLOAD, bytes.size)
            bytes
        }

    private class UploadTimeoutException : Exception("The upload did not finish in $UPLOAD_TIMEOUT")

    companion object {
        private const val TAG = "ProfilePhotoStorage"
        private const val CONTENT_TYPE = "image/webp"
        // A path never changes, so whoever caches a photo may keep it for good.
        private const val CACHE_CONTROL = "public, max-age=31536000, immutable"
        /** What `storage.rules` accepts, and so the most a download can ever be. */
        const val MAX_BYTES = 100L * 1024
        private val UPLOAD_TIMEOUT = 30.seconds
    }
}
