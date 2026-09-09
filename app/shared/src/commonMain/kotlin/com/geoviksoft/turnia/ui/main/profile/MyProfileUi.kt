package com.geoviksoft.turnia.ui.main.profile

import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.DrawableResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.animal_icon_bat

/**
 * The email is shown but never edited: it comes from the auth provider, not from the profile.
 */
data class MyProfileUi(
    val name: String = "",
    val username: String = "",
    val email: String = "",
    val nameError: ProfileFieldError? = null,
    val usernameError: ProfileFieldError? = null,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val userMessage: ProfileMessage? = null,
    val avatarDrawable: DrawableResource = Res.drawable.animal_icon_bat,
    val avatarBackground: Color = Color.Red
) {
    val canSave: Boolean
        get() = !saving && nameError == null && usernameError == null &&
                name.isNotBlank() && username.isNotBlank()
}

enum class ProfileFieldError { NameRequired, UsernameInvalid, UsernameTaken }

enum class ProfileMessage { SaveFailed }
