package com.georgevik.turnia.navigation.main.routes

import androidx.navigation3.runtime.NavKey
import com.georgevik.turnia.navigation.routes.EventTypeDetailData
import kotlinx.serialization.Serializable

@Serializable
sealed interface MainRoute : NavKey {
    @Serializable
    data object CalendarTab : MainRoute

    @Serializable
    data object GroupsTab : MainRoute

    @Serializable
    data object ProfileTab : MainRoute

    @Serializable
    data object ChangesTab : MainRoute

    @Serializable
    data class EventMasterKey(val groupId: String, val groupName: String) : MainRoute

    @Serializable
    data class EventTypeDetailKey(val data: EventTypeDetailData) : MainRoute

    @Serializable
    data class ExternalCalendar(val data: ExternalCalendarData) : MainRoute
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


@Serializable
enum class EventTypeKind { GROUP, PERSONAL }
