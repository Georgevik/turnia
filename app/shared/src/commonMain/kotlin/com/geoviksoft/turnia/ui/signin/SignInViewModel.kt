package com.geoviksoft.turnia.ui.signin

import androidx.lifecycle.ViewModel
import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.ui.signin.model.SignInError
import com.geoviksoft.turnia.ui.signin.model.SignInUi
import com.mmk.kmpauth.core.auth.KMPAuthUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val TAG = "SignInViewModel"

class SignInViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SignInUi())
    val uiState: StateFlow<SignInUi> = _uiState.asStateFlow()

    fun onSignInStarted() = _uiState.update { it.copy(signingIn = true, userMessage = null) }

    fun onSignInResult(result: Result<KMPAuthUser>) {
        result.fold(
            onSuccess = { },
            onFailure = { error ->
                Logger.e(TAG, "Sign-in failed", error)
                _uiState.update { it.copy(signingIn = false, userMessage = SignInError.Failed) }
            },
        )
    }

    fun userMessageShown() = _uiState.update { it.copy(userMessage = null) }
}
