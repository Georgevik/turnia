package com.georgevik.turnia.ui.main.group.calendarlist.model

import com.georgevik.turnia.core.domain.model.GroupId

data class GroupRowUi(
    val id: GroupId,
    val name: String,
    val members: Int
)