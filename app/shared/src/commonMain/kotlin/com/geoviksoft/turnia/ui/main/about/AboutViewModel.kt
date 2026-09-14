package com.geoviksoft.turnia.ui.main.about

import androidx.lifecycle.ViewModel
import com.geoviksoft.turnia.core.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AboutViewModel(userRepository: UserRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AboutUi(userId = userRepository.loggedUser?.id?.value.orEmpty())
    )
    val uiState: StateFlow<AboutUi> = _uiState.asStateFlow()
}
