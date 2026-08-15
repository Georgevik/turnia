package com.georgevik.turnia.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.UserSession
import com.georgevik.turnia.core.domain.repository.UserRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class SplashViewModel(
    private val userRepository: UserRepository
) : ViewModel() {
    private val _uiEvent = Channel<SplashUiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        viewModelScope.launch {
            userRepository.userSession.collect {
                val event = when (it) {
                    is UserSession.Authenticated -> SplashUiEvent.UserLoaded
                    UserSession.Unauthenticated -> SplashUiEvent.NewUser
                    UserSession.Loading -> null
                }

                Logger.d(TAG, "UserSession: $it")

                event?.let { _uiEvent.send(event) }
            }
        }
    }

    companion object {
        private const val TAG = "SplashViewModel"
    }
}

sealed interface SplashUiEvent {
    data object UserLoaded : SplashUiEvent
    data object NewUser : SplashUiEvent
    data class Error(val message: String) : SplashUiEvent
}
