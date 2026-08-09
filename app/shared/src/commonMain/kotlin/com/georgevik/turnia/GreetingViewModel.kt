package com.georgevik.turnia

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.GoogleSignInError
import com.georgevik.turnia.core.domain.model.GoogleSignInResult
import com.georgevik.turnia.core.domain.repo.AuthRepository
import com.georgevik.turnia.core.sayHello
import com.georgevik.turnia.interfaces.AuthProvider
import com.georgevik.turnia.interfaces.getPlatform
import kotlinx.coroutines.launch

private const val TAG = "GreetingViewModel"

class GreetingViewModel(
    private val authRepository: AuthRepository,
    private val authProvider: AuthProvider,
) : ViewModel() {

    /** Last Google sign-in error, for the UI to react to; `null` when none. */
    var lastSignInError by mutableStateOf<GoogleSignInError?>(null)
        private set

    fun signIn() {
        viewModelScope.launch {
            when (val result = authProvider.getGoogleToken()) {
                is GoogleSignInResult.Success -> {
                    lastSignInError = null
                    authRepository.signWithGoogle(result.token)
                }
                is GoogleSignInResult.Failure -> {
                    lastSignInError = result.error
                    Logger.e(TAG, "Google sign-in failed: ${result.error}")
                }
            }
        }
    }

    private val platform = getPlatform()

    val message: String = sayHello(platform.name)
}
