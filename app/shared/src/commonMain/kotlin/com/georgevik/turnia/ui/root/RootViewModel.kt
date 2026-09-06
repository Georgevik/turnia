package com.georgevik.turnia.ui.root

import androidx.lifecycle.ViewModel
import com.georgevik.turnia.core.domain.model.PushDestination
import com.georgevik.turnia.core.domain.repository.NotificationRepository
import com.georgevik.turnia.core.domain.repository.UserRepository
import kotlinx.coroutines.flow.StateFlow

class RootViewModel(
    userRepository: UserRepository,
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    val userSession = userRepository.userSession

    val pendingDestination: StateFlow<PushDestination?> = notificationRepository.pendingDestination

    fun destinationHandled() = notificationRepository.destinationHandled()
}
