package com.georgevik.turnia.ui.main

import androidx.lifecycle.ViewModel
import com.georgevik.turnia.core.domain.model.FeatureFlags
import com.georgevik.turnia.core.domain.model.PushDestination
import com.georgevik.turnia.core.domain.repository.AppConfigRepository
import com.georgevik.turnia.core.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * Holds app-wide state Main needs
 */
class MainViewModel(
    appConfigRepository: AppConfigRepository,
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    val featureFlags: StateFlow<FeatureFlags> = appConfigRepository.featureFlags

    val pendingDestination: StateFlow<PushDestination?> = notificationRepository.pendingDestination

    fun destinationHandled() = notificationRepository.destinationHandled()
}
