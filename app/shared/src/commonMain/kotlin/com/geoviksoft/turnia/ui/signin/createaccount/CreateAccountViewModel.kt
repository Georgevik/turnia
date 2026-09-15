package com.geoviksoft.turnia.ui.signin.createaccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoviksoft.turnia.core.domain.model.EmailAuthError
import com.geoviksoft.turnia.core.domain.repository.UserRepository
import com.geoviksoft.turnia.core.system.fold
import com.geoviksoft.turnia.ui.signin.model.isEmailShaped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CreateAccountField {
    Name, Email, Password, Terms, Privacy
}

class CreateAccountViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateAccountUi())
    val uiState: StateFlow<CreateAccountUi> = _uiState.asStateFlow()

    fun onFieldChanged(field: CreateAccountField, value: Any) {
        when (field) {
            CreateAccountField.Name -> _uiState.update {
                it.copy(
                    name = value as String, error = null
                )
            }
            CreateAccountField.Email -> _uiState.update {
                it.copy(
                    email = value as String, error = null
                )
            }
            CreateAccountField.Password -> _uiState.update {
                it.copy(
                    password = value as String, error = null
                )
            }
            CreateAccountField.Terms -> _uiState.update { it.copy(termsAccepted = value as Boolean) }
            CreateAccountField.Privacy -> _uiState.update { it.copy(privacyAccepted = value as Boolean) }
        }
    }

    /** Success needs no handling: the new session replaces the whole stack with Main. */
    fun onSubmit() {
        val state = _uiState.value
        if (!state.canSubmit) return
        if (!state.email.isEmailShaped()) return _uiState.update { it.copy(error = CreateAccountError.InvalidEmail) }

        _uiState.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            userRepository.createAccountWithEmail(
                state.name.trim(), state.email.trim(), state.password
            ).fold(
                onSuccess = { },
                onFailure = { failure ->
                    _uiState.update {
                        it.copy(
                            submitting = false, error = failure.toCreateAccountError()
                        )
                    }
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
