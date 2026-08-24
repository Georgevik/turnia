package com.georgevik.turnia.ui.signin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.UserSession
import com.georgevik.turnia.core.domain.repository.UserRepository
import com.mmk.kmpauth.core.auth.KMPAuthUser
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

private const val TAG = "GreetingViewModel"

class SignInViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    /** Signed-in Firebase user (KMPAuth already completed the Firebase sign-in). */
    var signedInUser by mutableStateOf<KMPAuthUser?>(null)
        private set

    /** Last sign-in error message, for the UI; `null` when none. */
    var lastSignInError by mutableStateOf<String?>(null)
        private set

    private val _signedIn = Channel<Unit>(Channel.CONFLATED)
    val signedIn = _signedIn.receiveAsFlow()

    init {
        viewModelScope.launch {
            userRepository.userSession.collect { session ->
                if (session is UserSession.Authenticated) _signedIn.send(Unit)
            }
        }
    }

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
