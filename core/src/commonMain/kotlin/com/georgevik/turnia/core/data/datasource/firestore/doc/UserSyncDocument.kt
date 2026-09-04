package com.georgevik.turnia.core.data.datasource.firestore.doc

import dev.gitlive.firebase.firestore.BaseTimestamp
import kotlinx.datetime.YearMonth
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserSyncDocument(
    @SerialName("personalEvents") val personalEventsUpdatedAt: Map<YearMonth, EventSyncUpdateAt> = emptyMap(),
    @SerialName("personalEventTypesUpdatedAt") val personalEventTypesUpdatedAt: BaseTimestamp? = null,
)
