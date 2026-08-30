package com.georgevik.turnia.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.domain.model.UserSession
import com.georgevik.turnia.core.domain.repository.AppConfigRepository
import com.georgevik.turnia.core.domain.repository.UserRepository
import com.georgevik.turnia.navigation.root.routes.RootRoute
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class SplashViewModel(
    private val appConfigRepository: AppConfigRepository,
    userRepository: UserRepository,
) : ViewModel() {
    private val _uiEvent = Channel<SplashUiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    private val startMark = TimeSource.Monotonic.markNow()

    init {
        viewModelScope.launch {
            appConfigRepository.refreshFeatureFlags()
        }

        viewModelScope.launch {
            val remaining = MIN_SPLASH_DURATION - startMark.elapsedNow()
            if (remaining.isPositive()) delay(remaining)

            val session = userRepository.userSession.first { it !is UserSession.Loading }
            _uiEvent.send(
                SplashUiEvent.Navigate(
                    when (session) {
                        is UserSession.Authenticated -> RootRoute.MainKey
                        else -> RootRoute.SignInKey
                    }
                )
            )
        }
    }

    companion object {
        private val MIN_SPLASH_DURATION = 1.seconds
    }
}

sealed interface SplashUiEvent {
    data class Navigate(val destination: RootRoute) : SplashUiEvent
    data class Error(val message: String) : SplashUiEvent
}
