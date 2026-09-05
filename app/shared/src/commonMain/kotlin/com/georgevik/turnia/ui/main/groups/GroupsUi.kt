package com.georgevik.turnia.ui.main.groups

import com.georgevik.turnia.ui.main.groups.model.GroupRowUi

sealed interface GroupsUi {

    data object Loading : GroupsUi

    data class Success(
        val groups: List<GroupRowUi> = emptyList(),
        val joinCode: String = "",
        val joinInProgress: Boolean = false,
        val userMessage: GroupsMessage? = null,
    ) : GroupsUi
}

enum class GroupsMessage {
    LoadFailed,
    Joined,
    JoinRequested,
    AlreadyMember,
    JoinCodeNotFound,
    JoinInvitationInactive,
    JoinInvitationExpired,
    JoinFailed,
}
