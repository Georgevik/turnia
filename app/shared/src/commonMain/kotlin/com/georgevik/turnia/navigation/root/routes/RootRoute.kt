package com.georgevik.turnia.navigation.root.routes

import androidx.navigation3.runtime.NavKey
import com.georgevik.turnia.navigation.routes.EventTypeDetailData
import kotlinx.serialization.Serializable

@Serializable
sealed interface RootRoute : NavKey {
    @Serializable
    data object SplashKey : RootRoute

    @Serializable
    data object SignInKey : RootRoute

    @Serializable
    data object MainKey : RootRoute

    /** Group detail/edit, full screen over Main. A blank [groupId] creates a new group. */
    @Serializable
    data class GroupDetailKey(val groupId: String) : RootRoute

    @Serializable
    data class EventTypeDetailKey(val data: EventTypeDetailData) : RootRoute

    /** The user's own event types, full screen over Main. */
    @Serializable
    data object PersonalEventTypesKey : RootRoute
}
