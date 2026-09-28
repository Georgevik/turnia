package com.geoviksoft.turnia.navigation.routes

import kotlinx.serialization.Serializable

/**
 * Payload of the event type detail screen. It lives outside `main`/`root` because the screen is
 * reachable from both back stacks: from the event type master inside Main, and from the
 * full-screen group detail hosted at root level.
 */
@Serializable
sealed interface EventTypeDetailData {
    @Serializable
    data class EditPersonal(val typeId: String) : EventTypeDetailData

    @Serializable
    data object NewPersonal : EventTypeDetailData

    /** A new personal type seeded from a one-off event: "Save as shift". Times are `HH:mm`. */
    @Serializable
    data class NewPersonalFrom(
        val name: String,
        val color: String,
        val startTime: String?,
        val endTime: String?,
    ) : EventTypeDetailData

    /** A null [groupId] means the type is a draft of a group that is still being created. */
    @Serializable
    data class EditGroup(val typeId: String, val groupId: String?) : EventTypeDetailData

    /** A null [groupId] means the group is still being created — see `pendingEventTypes`. */
    @Serializable
    data class NewGroup(val groupId: String?) : EventTypeDetailData
}
