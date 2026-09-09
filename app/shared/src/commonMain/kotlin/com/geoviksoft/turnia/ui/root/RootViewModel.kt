package com.geoviksoft.turnia.ui.root

import androidx.lifecycle.ViewModel
import com.geoviksoft.turnia.core.domain.repository.UserRepository

class RootViewModel(userRepository: UserRepository) : ViewModel() {

    val userSession = userRepository.userSession
}
