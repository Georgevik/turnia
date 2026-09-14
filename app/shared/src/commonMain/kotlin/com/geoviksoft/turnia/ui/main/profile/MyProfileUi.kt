package com.geoviksoft.turnia.ui.main.profile

/**
 * The email is shown but never edited: it comes from the auth provider, not from the profile.
 */
data class MyProfileUi(
    val name: String = "",
    val username: String = "",
    val email: String = "",
    val animalIconId: String? = null,
    val backgroundColor: String? = null,
    val pickingAvatar: Boolean = false,
    val nameError: ProfileFieldError? = null,
    val usernameError: ProfileFieldError? = null,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val deletingAccount: Boolean = false,
    val userMessage: ProfileMessage? = null,
) {
    val canSave: Boolean
        get() = !saving && nameError == null && usernameError == null &&
                name.isNotBlank() && username.isNotBlank()
}

enum class ProfileFieldError { NameRequired, UsernameInvalid, UsernameTaken }

enum class ProfileMessage { SaveFailed, DeleteAccountLastAdmin, DeleteAccountFailed }
