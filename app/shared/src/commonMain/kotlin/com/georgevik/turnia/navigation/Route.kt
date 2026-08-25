package com.georgevik.turnia.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation 3 back-stack keys. Each key identifies a destination and is
 * `@Serializable` so the back stack survives configuration changes and
 * process death via `rememberNavBackStack`.
 */
@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object SignInKey : Route

    @Serializable
    data object MainTabKey : Route

    @Serializable
    data object SpashKey : Route

    @Serializable
    data class EventMasterKey(val groupId: String, val groupName: String) : Route

    @Serializable
    data class EventTypeDetailKey(
        val kind: EventTypeKind,
        val groupId: String?,
        val typeId: String?,
    ) : Route
}

@Serializable
enum class EventTypeKind { GROUP, PERSONAL }
