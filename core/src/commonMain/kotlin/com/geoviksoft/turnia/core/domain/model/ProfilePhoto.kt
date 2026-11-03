package com.geoviksoft.turnia.core.domain.model

/**
 * A user's profile photo in Cloud Storage. A new photo is a new [photoId], so the file at [path]
 * never changes and whatever caches it may keep it forever.
 */
data class ProfilePhoto(val userId: UserId, val photoId: String) {
    val path: String get() = "profilePhotos/${userId.value}/$photoId.webp"

    /** What a cache keys it by: the same for the same file, on every device. */
    val key: String get() = "${userId.value}/$photoId"
}
