package com.geoviksoft.turnia.ui.main.people.model

import com.geoviksoft.turnia.core.domain.model.UserId

/**
 * Someone on either side of a calendar grant. [name] and [username] are blank when that user has
 * no username reservation to resolve them from.
 */
data class PersonRowUi(
    val id: UserId,
    val name: String,
    val username: String,
)
