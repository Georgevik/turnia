package com.georgevik.turnia.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class SplashViewModel : ViewModel() {
    private val _uiEvent = Channel<SplashUiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        viewModelScope.launch {
            loadUser()
        }
    }

    private suspend fun loadUser() {
        _uiEvent.send(SplashUiEvent.UserLoaded)
    }
}

sealed interface SplashUiEvent {
    data object UserLoaded : SplashUiEvent
    data object NewUser : SplashUiEvent
    data class Error(val message: String) : SplashUiEvent
}
