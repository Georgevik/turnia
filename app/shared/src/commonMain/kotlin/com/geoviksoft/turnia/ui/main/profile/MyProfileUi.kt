package com.geoviksoft.turnia.ui.main.profile

import androidx.compose.ui.graphics.ImageBitmap
import com.geoviksoft.turnia.core.domain.model.ProfilePhoto

/**
 * The email is shown but never edited: it comes from the auth provider, not from the profile.
 *
 * The photo is not part of what *Save* stores: uploading or removing it takes effect at once, and
 * leaves the name, the username and the animal as the user left them.
 */
data class MyProfileUi(
    val name: String = "",
    val username: String = "",
    val email: String = "",
    val animalIconId: String? = null,
    val backgroundColor: String? = null,
    val photo: ProfilePhoto? = null,
    val avatarMenuOpen: Boolean = false,
    val pickingAvatar: Boolean = false,
    val photoStep: PhotoStep = PhotoStep.Idle,
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

/** Where a new photo is, from the moment it was picked until it is the profile's. */
sealed interface PhotoStep {
    data object Idle : PhotoStep

    /** Decoding the picked image, which can take a moment for a large one. */
    data object Preparing : PhotoStep

    data class Cropping(val image: ImageBitmap) : PhotoStep

    data object Uploading : PhotoStep

    data object Removing : PhotoStep

    val busy: Boolean get() = this == Preparing || this == Uploading || this == Removing
}

enum class ProfileFieldError { NameRequired, UsernameInvalid, UsernameTaken }

enum class ProfileMessage {
    SaveFailed,
    DeleteAccountLastAdmin,
    DeleteAccountFailed,
    PhotoUnreadable,
    PhotoUploadFailed,
    PhotoRemoveFailed,
}
