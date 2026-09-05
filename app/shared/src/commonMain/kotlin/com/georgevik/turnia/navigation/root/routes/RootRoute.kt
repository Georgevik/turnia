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

    @Serializable
    data class GroupDetailKey(val groupId: String) : RootRoute

    @Serializable
    data class EventTypeDetailKey(val data: EventTypeDetailData) : RootRoute

    @Serializable
    data object PersonalEventTypesKey : RootRoute

    @Serializable
    data object MyProfileKey : RootRoute

    @Serializable
    data object ShareCalendarKey : RootRoute

    @Serializable
    data object AdminGroupsKey : RootRoute

}
