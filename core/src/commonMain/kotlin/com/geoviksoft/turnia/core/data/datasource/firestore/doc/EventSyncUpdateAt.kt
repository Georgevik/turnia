package com.geoviksoft.turnia.core.data.datasource.firestore.doc

import dev.gitlive.firebase.firestore.BaseTimestamp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EventSyncUpdateAt(
    @SerialName("updatedAt") val updatedAt: BaseTimestamp? = null,
)
