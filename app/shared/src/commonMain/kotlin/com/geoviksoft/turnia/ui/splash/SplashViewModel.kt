package com.geoviksoft.turnia.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoviksoft.turnia.core.domain.model.UserSession
import com.geoviksoft.turnia.core.domain.repository.AppConfigRepository
import com.geoviksoft.turnia.core.domain.repository.InvitationLinkRepository
import com.geoviksoft.turnia.core.domain.repository.UserRepository
import com.geoviksoft.turnia.navigation.root.routes.RootRoute
import com.geoviksoft.turnia.ui.system.ads.AdConsent
import com.geoviksoft.turnia.ui.system.ads.AdConsentPlatform
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class SplashViewModel(
    private val appConfigRepository: AppConfigRepository,
    userRepository: UserRepository,
    private val adConsent: AdConsent,
    private val invitations: InvitationLinkRepository,
) : ViewModel() {
    private val _uiEvent = Channel<SplashUiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    /** The consent message was not answered: the splash waits on the user instead of loading. */
    val consentMissing: StateFlow<Boolean> =
        combine(adConsent.status, adConsent.gathering) { status, gathering ->
            status != null && !status.canRequestAds && !gathering
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = false)

    private val startMark = TimeSource.Monotonic.markNow()

    init {
        viewModelScope.launch {
            appConfigRepository.refreshFeatureFlags()
        }

        viewModelScope.launch {
            val remaining = MIN_SPLASH_DURATION - startMark.elapsedNow()
            if (remaining.isPositive()) delay(remaining)

            val session =
                withTimeoutOrNull(TIMEOUT_SESSION) { userRepository.userSession.first { it !is UserSession.Loading } }
            // No way past the splash without an answer: the app is paid for by its ads.
            adConsent.status.first { it?.canRequestAds == true }
            _uiEvent.send(
                SplashUiEvent.Navigate(
                    when {
                        session is UserSession.Authenticated -> RootRoute.MainKey
                        invitations.pendingCode.value != null -> RootRoute.SignInKey
                        appConfigRepository.isOnboardingSeen() -> RootRoute.SignInKey
                        else -> RootRoute.OnboardingKey
                    }
                )
            )
        }
    }

    fun gatherConsent(platform: AdConsentPlatform) = adConsent.gather(platform)

    companion object {
        private val MIN_SPLASH_DURATION = 1.seconds
        private val TIMEOUT_SESSION = 10.seconds
    }
}

sealed interface SplashUiEvent {
    data class Navigate(val destination: RootRoute) : SplashUiEvent
    data class Error(val message: String) : SplashUiEvent
}
