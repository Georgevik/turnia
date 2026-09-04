package com.georgevik.turnia.ui.main.groups

import com.georgevik.turnia.ui.main.groups.model.GroupRowUi

sealed interface GroupsUi {

    data object Loading : GroupsUi

    data class Success(
        val query: String = "",
        val groups: List<GroupRowUi> = emptyList(),
        val userMessage: GroupsMessage? = null,
    ) : GroupsUi
}

enum class GroupsMessage { LoadFailed }
