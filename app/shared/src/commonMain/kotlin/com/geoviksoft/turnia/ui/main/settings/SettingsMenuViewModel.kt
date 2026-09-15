package com.geoviksoft.turnia.ui.main.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoviksoft.turnia.core.domain.model.Membership
import com.geoviksoft.turnia.core.domain.model.UserSession
import com.geoviksoft.turnia.core.domain.repository.AppConfigRepository
import com.geoviksoft.turnia.core.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsMenuViewModel(
    private val userRepository: UserRepository,
    private val appConfigRepository: AppConfigRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsMenuUi())
    val uiState: StateFlow<SettingsMenuUi> = _uiState.asStateFlow()

    init {
        loadUser()
    }

    fun loadUser() {
        viewModelScope.launch {
            combine(
                userRepository.userSession.filterIsInstance<UserSession.Authenticated>(),
                appConfigRepository.featureFlags,
            ) { session, flags -> session to flags }
                .collect { (session, flags) ->
                    _uiState.update {
                        it.copy(
                            adsEnabled = flags.enableAds && flags.enableSubscription,
                            userDetails = SettingsMenuUi.UserDetails(
                                displayName = session.user.displayName.orEmpty(),
                                username = session.user.username,
                                isPremium = session.user.membership == Membership.PREMIUM,
                                avatar = session.user.avatar,
                            )
                        )
                    }
                }


        }
    }

    fun onLogoutClicked() {
        viewModelScope.launch {
            userRepository.signOut()
        }
    }
}
