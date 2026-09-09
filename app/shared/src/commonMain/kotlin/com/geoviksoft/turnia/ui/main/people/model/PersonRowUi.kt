package com.geoviksoft.turnia.ui.main.people.model

import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.domain.model.UserProfile

/**
 * Someone on either side of a calendar grant
 */
data class PersonRowUi(
    val id: UserId,
    val name: String,
    val username: String,
    val avatar: UserProfile.AnimalAvatar = UserProfile.AnimalAvatar.NONE,
)
