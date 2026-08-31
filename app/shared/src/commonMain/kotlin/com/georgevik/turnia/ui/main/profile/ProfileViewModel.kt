package com.georgevik.turnia.ui.main.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.domain.repository.UserRepository
import kotlinx.coroutines.launch

class ProfileViewModel(private val userRepository: UserRepository) : ViewModel() {
    fun onLogoutClicked() {
        viewModelScope.launch {
            userRepository.signOut()
        }
    }
}
