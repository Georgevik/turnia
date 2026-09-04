package com.georgevik.turnia.ui.main.people.model

import com.georgevik.turnia.core.domain.model.UserId

/** Someone who shared their calendar with this user. */
data class ColleagueRowUi(
    val id: UserId,
    val name: String,
    val username: String,
)
