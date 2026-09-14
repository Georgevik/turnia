package com.geoviksoft.turnia.ui.main.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoviksoft.turnia.core.domain.model.DeleteAccountError
import com.geoviksoft.turnia.core.domain.model.UsernameError
import com.geoviksoft.turnia.core.domain.repository.UserRepository
import com.geoviksoft.turnia.core.domain.username.UsernameFactory
import com.geoviksoft.turnia.core.system.fold
import com.geoviksoft.turnia.core.system.onFailure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MyProfileViewModel(
    private val userRepository: UserRepository,
    private val usernameFactory: UsernameFactory,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyProfileUi())
    val uiState: StateFlow<MyProfileUi> = _uiState.asStateFlow()

    /** What the screen opened with, so saving can skip the halves that did not move. */
    private var initProfileUi: MyProfileUi = MyProfileUi()

    init {
        viewModelScope.launch {
            userRepository.loggedUserFlow.collect { user ->

                _uiState.update {
                    it.copy(
                        name = user.displayName.orEmpty(),
                        username = user.username,
                        email = user.email.orEmpty(),
                        animalIconId = user.avatar.animal,
                        backgroundColor = user.avatar.background,
                    )
                }
                initProfileUi = _uiState.value
            }
        }
    }

    fun onAvatarClicked() = _uiState.update { it.copy(pickingAvatar = true) }

    fun onAvatarPickerDismissed() = _uiState.update { it.copy(pickingAvatar = false) }

    fun onAnimalPicked(animalIconId: String) =
        _uiState.update { it.copy(animalIconId = animalIconId) }

    fun onBackgroundPicked(backgroundColor: String) =
        _uiState.update { it.copy(backgroundColor = backgroundColor) }

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

            if (state.avatarChanged()) {
                val stored = userRepository
                    .updateAvatar(state.animalIconId, state.backgroundColor)
                    .fold(onSuccess = { true }, onFailure = { false })

                if (!stored) {
                    _uiState.update {
                        it.copy(saving = false, userMessage = ProfileMessage.SaveFailed)
                    }
                    return@launch
                }
            }

            if (!state.profileChanged()) {
                _uiState.update { it.copy(saving = false, saved = true) }
                return@launch
            }

            userRepository.updateProfile(state.name.trim(), state.username).fold(
                onSuccess = { _uiState.update { it.copy(saving = false, saved = true) } },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(saving = false).withError(error)
                    }
                },
            )
        }
    }

    private fun MyProfileUi.avatarChanged(): Boolean =
        animalIconId != initProfileUi.animalIconId || backgroundColor != initProfileUi.backgroundColor

    private fun MyProfileUi.profileChanged(): Boolean =
        name.trim() != initProfileUi.name.trim() || username != initProfileUi.username

    fun onDeleteAccountConfirmed() {
        if (_uiState.value.deletingAccount) return
        _uiState.update { it.copy(deletingAccount = true) }

        // Success needs no handling here: the session ends, and the root takes the app to sign-in.
        viewModelScope.launch {
            userRepository.deleteAccount().onFailure { error ->
                val message = when (error) {
                    DeleteAccountError.LastAdmin -> ProfileMessage.DeleteAccountLastAdmin
                    DeleteAccountError.Failed -> ProfileMessage.DeleteAccountFailed
                }
                _uiState.update { it.copy(deletingAccount = false, userMessage = message) }
            }
        }
    }

    fun userMessageShown() = _uiState.update { it.copy(userMessage = null) }

    private fun MyProfileUi.withError(error: UsernameError): MyProfileUi = when (error) {
        UsernameError.Invalid -> copy(usernameError = ProfileFieldError.UsernameInvalid)
        UsernameError.Taken -> copy(usernameError = ProfileFieldError.UsernameTaken)
        UsernameError.SaveFailed -> copy(userMessage = ProfileMessage.SaveFailed)
    }
}
