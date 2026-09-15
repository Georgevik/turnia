package com.geoviksoft.turnia.core.domain.model

/** What any signed-in user can read about this one: the name, the handle and the avatar. */
data class UserProfile(
    val id: UserId,
    val name: String,
    val username: String,
    val avatar: AnimalAvatar = AnimalAvatar.NONE,
    val showAds: Boolean = false,
) {
    data class AnimalAvatar(
        val animal: String?,
        val background: String?,
    ) {
        companion object {
            val NONE = AnimalAvatar(animal = null, background = null)
            val PREVIEW = AnimalAvatar(animal = "bat", background = "#488844")
        }
    }
}
