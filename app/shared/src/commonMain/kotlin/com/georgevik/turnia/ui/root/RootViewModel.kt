package com.georgevik.turnia.ui.root

import androidx.lifecycle.ViewModel
import com.georgevik.turnia.core.domain.repository.UserRepository

class RootViewModel(
    userRepository: UserRepository
) : ViewModel() {

    val userSession = userRepository.userSession

}
