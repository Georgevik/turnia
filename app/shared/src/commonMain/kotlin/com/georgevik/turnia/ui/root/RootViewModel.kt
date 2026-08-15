package com.georgevik.turnia.ui.root

import androidx.lifecycle.ViewModel
import com.georgevik.turnia.core.domain.repository.UserRepository

class RootViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    val userSession = userRepository.userSession

}
