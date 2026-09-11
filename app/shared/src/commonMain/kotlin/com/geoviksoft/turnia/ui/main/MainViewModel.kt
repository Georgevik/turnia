package com.geoviksoft.turnia.ui.main

import androidx.lifecycle.ViewModel
import com.geoviksoft.turnia.core.domain.model.PushDestination
import com.geoviksoft.turnia.core.domain.repository.InvitationLinkRepository
import com.geoviksoft.turnia.core.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * Holds app-wide state Main needs
 */
class MainViewModel(
    private val notificationRepository: NotificationRepository,
    invitationLinkRepository: InvitationLinkRepository,
) : ViewModel() {

    val pendingDestination: StateFlow<PushDestination?> = notificationRepository.pendingDestination

    /** Only watched to bring the Groups tab up: the tab itself takes the code. */
    val pendingJoinCode: StateFlow<String?> = invitationLinkRepository.pendingCode

    fun destinationHandled() = notificationRepository.destinationHandled()
}
