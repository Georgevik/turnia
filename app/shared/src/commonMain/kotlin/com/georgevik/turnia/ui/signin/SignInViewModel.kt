package com.georgevik.turnia.ui.signin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.georgevik.turnia.core.data.logger.Logger
import com.mmk.kmpauth.core.auth.KMPAuthUser

private const val TAG = "GreetingViewModel"

/**
 * Post-auth navigation to Main is decided centrally in `RootScreen.kt`'s `App()`, driven by
 * [com.georgevik.turnia.core.domain.model.UserSession] — this view model only tracks local UI
 * feedback (the signed-in user / last error), not navigation.
 */
class SignInViewModel : ViewModel() {

    /** Signed-in Firebase user (KMPAuth already completed the Firebase sign-in). */
    var signedInUser by mutableStateOf<KMPAuthUser?>(null)
        private set

    /** Last sign-in error message, for the UI; `null` when none. */
    var lastSignInError by mutableStateOf<String?>(null)
        private set

    fun onSignInResult(result: Result<KMPAuthUser>) {
        result.onSuccess { user ->
            signedInUser = user
            lastSignInError = null
            Logger.i(TAG, "Signed in: ${user.uid}")
        }.onFailure { error ->
            lastSignInError = error.message ?: "Unknown error"
            Logger.e(TAG, "Google sign-in failed", error)
        }
    }
}
