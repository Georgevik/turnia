package com.geoviksoft.turnia.core.data.datasource.firestorefunctions.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A null [photoId] removes the photo. It has no default, so it is sent even when null. */
@Serializable
data class SetProfilePhotoRequest(
    @SerialName("photoId") val photoId: String?,
)
