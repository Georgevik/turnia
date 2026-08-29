package com.georgevik.turnia.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.domain.repository.AppConfigRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Purely cosmetic: runs the minimum splash-duration timer and warms up feature flags.
 * Routing (Main vs. SignIn) is decided centrally in `RootScreen.kt`'s `App()`, driven by
 * [com.georgevik.turnia.core.domain.model.UserSession] — this view model has no opinion on it.
 */
class SplashViewModel(
    private val appConfigRepository: AppConfigRepository,
) : ViewModel() {
    private val _uiEvent = Channel<SplashUiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    private val startMark = TimeSource.Monotonic.markNow()

    init {
        // Download feature flags while the splash is on screen; the result is
        // cached in the repository and read later (e.g. to gate the Swap tab).
        viewModelScope.launch {
            appConfigRepository.refreshFeatureFlags()
        }

        viewModelScope.launch {
            val remaining = MIN_SPLASH_DURATION - startMark.elapsedNow()
            if (remaining.isPositive()) delay(remaining)
            _uiEvent.send(SplashUiEvent.Ready)
        }
    }

    companion object {
        private val MIN_SPLASH_DURATION = 1.seconds
    }
}

sealed interface SplashUiEvent {
    data object Ready : SplashUiEvent
    data class Error(val message: String) : SplashUiEvent
}
