package com.georgevik.turnia.ui.main.groups.model

import com.georgevik.turnia.core.domain.model.GroupId

data class GroupRowUi(
    val id: GroupId,
    val name: String,
    val members: Int,
    /** Only an admin can edit the group and accept the people asking to join it. */
    val isAdmin: Boolean,
)
