package com.georgevik.turnia.core.data.datasource.firestore.doc

import dev.gitlive.firebase.firestore.BaseTimestamp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserSyncDocument(
    @SerialName("personalEventsUpdatedAt") val personalEventsUpdatedAt: BaseTimestamp? = null,
    @SerialName("personalEventTypesUpdatedAt") val personalEventTypesUpdatedAt: BaseTimestamp? = null,
)
