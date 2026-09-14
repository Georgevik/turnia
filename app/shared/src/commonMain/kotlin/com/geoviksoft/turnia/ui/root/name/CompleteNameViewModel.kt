package com.geoviksoft.turnia.ui.root.name

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoviksoft.turnia.core.domain.repository.UserRepository
import com.geoviksoft.turnia.core.domain.username.UsernameFactory
import com.geoviksoft.turnia.core.system.fold
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CompleteNameUi(
    val name: String = "",
    val saving: Boolean = false,
    val error: CompleteNameError? = null,
)

enum class CompleteNameError { NameRequired, SaveFailed }

class CompleteNameViewModel(
    private val userRepository: UserRepository,
    private val usernameFactory: UsernameFactory,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompleteNameUi())
    val uiState: StateFlow<CompleteNameUi> = _uiState.asStateFlow()

    fun onNameChanged(name: String) = _uiState.update { it.copy(name = name, error = null) }

    /** Success needs no handling: the session carries the name, and the dialog is gone with it. */
    fun onContinue() {
        val state = _uiState.value
        if (state.saving) return
        val name = state.name.trim()
        if (name.isEmpty()) return _uiState.update { it.copy(error = CompleteNameError.NameRequired) }
        val user = userRepository.loggedUser ?: return

        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            // A claim that failed at sign-up leaves no username, and the profile cannot be saved without one.
            val username = user.username.ifBlank { usernameFactory.create(name) }
            userRepository.updateProfile(name, username).fold(
                onSuccess = { _uiState.value = CompleteNameUi() },
                onFailure = { _uiState.update { it.copy(saving = false, error = CompleteNameError.SaveFailed) } },
            )
        }
    }

    fun onCancel() {
        _uiState.value = CompleteNameUi()
        viewModelScope.launch { userRepository.signOut() }
    }
}
