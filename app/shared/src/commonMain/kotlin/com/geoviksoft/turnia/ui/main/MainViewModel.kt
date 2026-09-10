package com.geoviksoft.turnia.ui.main

import androidx.lifecycle.ViewModel
import com.geoviksoft.turnia.core.domain.model.PushDestination
import com.geoviksoft.turnia.core.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * Holds app-wide state Main needs
 */
class MainViewModel(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    val pendingDestination: StateFlow<PushDestination?> = notificationRepository.pendingDestination

    fun destinationHandled() = notificationRepository.destinationHandled()
}
