package com.georgevik.turnia.ui.main.group.calendarlist.model

import com.georgevik.turnia.core.domain.model.UserId

data class ColleageRowUi(
    val id: UserId,
    val name: String,
    val subtitle: String? = null,
)