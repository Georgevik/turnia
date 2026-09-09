package com.geoviksoft.turnia.core.data.datasource.firestore.doc

import dev.gitlive.firebase.firestore.BaseTimestamp
import kotlinx.datetime.YearMonth
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `groups/{groupId}/sync/updates` — when each month of a group's calendar last changed. One per
 * group and not per member: any member's write moves the month every member reads.
 */
@Serializable
data class GroupSyncDocument(
    @SerialName("events") val eventsUpdatedAt: Map<YearMonth, EventSyncUpdateAt> = emptyMap(),
    /** The group document itself: its name, its invitation and above all its event types. */
    @SerialName("group") val groupUpdatedAt: BaseTimestamp? = null,
)
