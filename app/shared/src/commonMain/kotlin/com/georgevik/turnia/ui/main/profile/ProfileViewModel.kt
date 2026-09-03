package com.georgevik.turnia.ui.main.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.domain.model.Membership
import com.georgevik.turnia.core.domain.model.UserSession
import com.georgevik.turnia.core.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileScreenUi())
    val uiState: StateFlow<ProfileScreenUi> = _uiState.asStateFlow()

    init {
        loadUser()
    }

    fun loadUser() {
        viewModelScope.launch {
            userRepository.userSession.filterIsInstance<UserSession.Authenticated>()
                .collect { session ->
                    _uiState.update {
                        it.copy(
                            userDetails = ProfileScreenUi.UserDetails(
                                displayName = session.user.displayName.orEmpty(),
                                username = session.user.username,
                                email = session.user.email.orEmpty(),
                                isPremium = session.user.membership == Membership.PREMIUM,
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
