package com.georgevik.turnia.navigation.main.routes

import androidx.navigation3.runtime.NavKey
import com.georgevik.turnia.core.domain.model.CalendarKind
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
    data class EventTypeDetailKey(
        val kind: EventTypeKind,
        val groupId: String?,
        val typeId: String?,
    ) : MainRoute

    @Serializable
    data class GroupCalendar(val id: String, val name: String, val kind: CalendarKind) : MainRoute
}

@Serializable
enum class EventTypeKind { GROUP, PERSONAL }
