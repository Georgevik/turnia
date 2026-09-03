package com.georgevik.turnia.ui.main.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.domain.model.UserSession
import com.georgevik.turnia.core.domain.model.UsernameError
import com.georgevik.turnia.core.domain.repository.UserRepository
import com.georgevik.turnia.core.domain.username.UsernameFactory
import com.georgevik.turnia.core.system.fold
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MyProfileViewModel(
    private val userRepository: UserRepository,
    private val usernameFactory: UsernameFactory,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyProfileUi())
    val uiState: StateFlow<MyProfileUi> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val user = userRepository.userSession
                .filterIsInstance<UserSession.Authenticated>().first().user

            _uiState.update {
                it.copy(
                    name = user.displayName.orEmpty(),
                    username = user.username,
                    email = user.email.orEmpty(),
                )
            }
        }
    }

    fun onNameChanged(name: String) = _uiState.update {
        it.copy(
            name = name,
            nameError = ProfileFieldError.NameRequired.takeIf { _ -> name.isBlank() },
        )
    }

    fun onUsernameChanged(username: String) {
        val sanitized = username.trim().lowercase()
        _uiState.update {
            it.copy(
                username = sanitized,
                usernameError = ProfileFieldError.UsernameInvalid
                    .takeIf { _ -> sanitized.isNotEmpty() && !usernameFactory.isValid(sanitized) },
            )
        }
    }

    fun onSave() {
        val state = _uiState.value
        if (!state.canSave) return

        viewModelScope.launch {
            _uiState.update { it.copy(saving = true) }

            userRepository.updateProfile(state.name.trim(), state.username).fold(
                onSuccess = { _uiState.update { it.copy(saving = false, saved = true) } },
                onFailure = { error -> _uiState.update { it.copy(saving = false).withError(error) } },
            )
        }
    }

    fun userMessageShown() = _uiState.update { it.copy(userMessage = null) }

    private fun MyProfileUi.withError(error: UsernameError): MyProfileUi = when (error) {
        UsernameError.Invalid -> copy(usernameError = ProfileFieldError.UsernameInvalid)
        UsernameError.Taken -> copy(usernameError = ProfileFieldError.UsernameTaken)
        UsernameError.SaveFailed -> copy(userMessage = ProfileMessage.SaveFailed)
    }
}
