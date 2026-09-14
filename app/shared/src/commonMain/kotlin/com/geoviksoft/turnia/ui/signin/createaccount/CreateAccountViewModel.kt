package com.geoviksoft.turnia.ui.signin.createaccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoviksoft.turnia.core.domain.model.EmailAuthError
import com.geoviksoft.turnia.core.domain.model.PasswordRule
import com.geoviksoft.turnia.core.domain.model.meetsPasswordPolicy
import com.geoviksoft.turnia.core.domain.repository.UserRepository
import com.geoviksoft.turnia.core.system.fold
import com.geoviksoft.turnia.ui.signin.model.isEmailShaped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateAccountUi(
    val email: String = "",
    val password: String = "",
    val submitting: Boolean = false,
    val error: CreateAccountError? = null,
) {
    val metRules: Set<PasswordRule> get() = PasswordRule.entries.filterTo(mutableSetOf()) { it.isMetBy(password) }
    val canSubmit: Boolean get() = !submitting && email.isNotBlank() && password.meetsPasswordPolicy()
}

enum class CreateAccountError { InvalidEmail, EmailInUse, WeakPassword, Failed }

class CreateAccountViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateAccountUi())
    val uiState: StateFlow<CreateAccountUi> = _uiState.asStateFlow()

    fun onEmailChanged(email: String) = _uiState.update { it.copy(email = email, error = null) }

    fun onPasswordChanged(password: String) = _uiState.update { it.copy(password = password, error = null) }

    /**
     * Success needs no handling: the new session replaces the whole stack with Main, and the
     * complete-name dialog asks for the name this account was born without.
     */
    fun onSubmit() {
        val state = _uiState.value
        if (!state.canSubmit) return
        if (!state.email.isEmailShaped()) return _uiState.update { it.copy(error = CreateAccountError.InvalidEmail) }

        _uiState.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            userRepository.createAccountWithEmail(state.email.trim(), state.password).fold(
                onSuccess = { },
                onFailure = { failure ->
                    _uiState.update { it.copy(submitting = false, error = failure.toCreateAccountError()) }
                },
            )
        }
    }

    private fun EmailAuthError.toCreateAccountError() = when (this) {
        EmailAuthError.EmailInUse -> CreateAccountError.EmailInUse
        EmailAuthError.WeakPassword -> CreateAccountError.WeakPassword
        // What a malformed address throws on sign-up.
        EmailAuthError.InvalidCredentials -> CreateAccountError.InvalidEmail
        EmailAuthError.Failed -> CreateAccountError.Failed
    }
}
