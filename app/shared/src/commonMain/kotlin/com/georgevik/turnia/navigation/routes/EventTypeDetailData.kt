package com.georgevik.turnia.navigation.routes

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

    @Serializable
    data class EditGroup(val typeId: String, val groupId: String) : EventTypeDetailData

    @Serializable
    data class NewGroup(val groupId: String) : EventTypeDetailData
}
