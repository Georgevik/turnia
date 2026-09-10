package com.geoviksoft.turnia.navigation.main.routes

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface MainRoute : NavKey {
    @Serializable
    data object CalendarTab : MainRoute

    @Serializable
    data object PeopleTab : MainRoute

    @Serializable
    data object GroupsTab : MainRoute

    @Serializable
    data object SettingsMenuTab : MainRoute

    @Serializable
    data object SwapTab : MainRoute

    @Serializable
    data class ExternalCalendar(val data: ExternalCalendarData) : MainRoute

    /** A null [groupId] opens the screen on a group that is still being created. */
    @Serializable
    data class GroupDetail(val groupId: String?) : MainRoute

    @Serializable
    data object AdminGroups : MainRoute
}

@Serializable
sealed class ExternalCalendarData {
    abstract val id: String
    abstract val name: String

    @Serializable
    data class Group(override val id: String, override val name: String) : ExternalCalendarData()

    @Serializable
    data class Personal(override val id: String, override val name: String) : ExternalCalendarData()
}
