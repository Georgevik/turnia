package com.geoviksoft.turnia.ui.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.core.domain.model.EmailAuthError
import com.geoviksoft.turnia.core.domain.repository.UserRepository
import com.geoviksoft.turnia.core.system.fold
import com.geoviksoft.turnia.ui.signin.model.EmailForm
import com.geoviksoft.turnia.ui.signin.model.EmailFormError
import com.geoviksoft.turnia.ui.signin.model.SignInError
import com.geoviksoft.turnia.ui.signin.model.SignInUi
import com.geoviksoft.turnia.ui.signin.model.isEmailShaped
import com.mmk.kmpauth.core.auth.KMPAuthUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "SignInViewModel"

class SignInViewModel(private val userRepository: UserRepository) : ViewModel() {

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

    fun onEmailSignInOpened() = _uiState.update { it.copy(emailForm = EmailForm()) }

    fun onEmailSignInDismissed() = _uiState.update {
        if (it.emailForm?.submitting == true) it else it.copy(emailForm = null)
    }

    fun onEmailChanged(email: String) = updateForm { copy(email = email, error = null, resetSentTo = null) }

    fun onPasswordChanged(password: String) = updateForm { copy(password = password, error = null) }

    /** Success needs no handling: the new session takes the app past this screen. */
    fun onEmailSubmit() {
        val form = _uiState.value.emailForm ?: return
        if (form.submitting) return
        val error = when {
            !form.email.isEmailShaped() -> EmailFormError.InvalidEmail
            form.password.isEmpty() -> EmailFormError.PasswordRequired
            else -> null
        }
        if (error != null) return updateForm { copy(error = error) }

        updateForm { copy(submitting = true, error = null, resetSentTo = null) }
        viewModelScope.launch {
            userRepository.signInWithEmail(form.email.trim(), form.password).fold(
                onSuccess = { },
                onFailure = { failure -> updateForm { copy(submitting = false, error = failure.toFormError()) } },
            )
        }
    }

    fun onPasswordResetRequested() {
        val form = _uiState.value.emailForm ?: return
        if (form.submitting) return
        val email = form.email.trim()
        if (!email.isEmailShaped()) return updateForm { copy(error = EmailFormError.InvalidEmail) }

        updateForm { copy(submitting = true, error = null, resetSentTo = null) }
        viewModelScope.launch {
            userRepository.sendPasswordReset(email).fold(
                onSuccess = { updateForm { copy(submitting = false, resetSentTo = email) } },
                onFailure = { updateForm { copy(submitting = false, error = EmailFormError.Failed) } },
            )
        }
    }

    private fun updateForm(transform: EmailForm.() -> EmailForm) = _uiState.update { state ->
        state.emailForm?.let { state.copy(emailForm = it.transform()) } ?: state
    }

    private fun EmailAuthError.toFormError() = when (this) {
        EmailAuthError.InvalidCredentials -> EmailFormError.InvalidCredentials
        EmailAuthError.EmailInUse, EmailAuthError.WeakPassword, EmailAuthError.Failed -> EmailFormError.Failed
    }
}
