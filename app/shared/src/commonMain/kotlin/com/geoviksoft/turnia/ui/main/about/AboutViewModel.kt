package com.geoviksoft.turnia.ui.main.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoviksoft.turnia.core.domain.repository.UserRepository
import com.geoviksoft.turnia.core.system.onFailure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AboutViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AboutUi(userId = userRepository.loggedUser?.id?.value.orEmpty())
    )
    val uiState: StateFlow<AboutUi> = _uiState.asStateFlow()

    fun onDeleteAccountConfirmed() {
        if (_uiState.value.deletingAccount) return
        _uiState.update { it.copy(deletingAccount = true) }

        // Success needs no handling here: the session ends, and the root takes the app to sign-in.
        viewModelScope.launch {
            userRepository.deleteAccount().onFailure { error ->
                _uiState.update { it.copy(deletingAccount = false, userMessage = error) }
            }
        }
    }

    fun userMessageShown() = _uiState.update { it.copy(userMessage = null) }
}
