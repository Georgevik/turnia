package com.georgevik.turnia.ui.main.settings.groups

import com.georgevik.turnia.ui.main.group.calendarlist.model.GroupRowUi

sealed interface GroupsUi {
    data object Loading : GroupsUi

    data class Success(
        val groups: List<GroupRowUi> = emptyList(),
        val userMessage: GroupsMessage? = null,
    ) : GroupsUi
}

enum class GroupsMessage { LoadFailed }
