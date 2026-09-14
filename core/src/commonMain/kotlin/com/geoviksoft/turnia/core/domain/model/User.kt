package com.geoviksoft.turnia.core.domain.model

data class User(
    val id: UserId,
    val email: String?,
    val displayName: String?,
    val username: String,
    val membership: Membership,
    val avatar: UserProfile.AnimalAvatar = UserProfile.AnimalAvatar.NONE,
    /** False while only Auth has answered: until the profile is read, a blank name proves nothing. */
    val hasProfile: Boolean = false,
) {
    /** Apple, and sometimes Google, sign a user in with no name, and the app will not go on without one. */
    val needsName: Boolean get() = hasProfile && displayName.isNullOrBlank()
}

enum class Membership {
    FREE, PREMIUM
}
