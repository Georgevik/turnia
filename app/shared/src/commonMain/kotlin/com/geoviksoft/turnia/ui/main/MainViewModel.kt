package com.geoviksoft.turnia.ui.main

import androidx.lifecycle.viewModelScope
import com.geoviksoft.turnia.core.domain.model.UserSession
import com.geoviksoft.turnia.core.domain.repository.ShiftSetupRepository
import com.geoviksoft.turnia.core.domain.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    shiftSetupRepository: ShiftSetupRepository,
    userRepository: UserRepository,
) : ViewModel() {

    val pendingDestination: StateFlow<PushDestination?> = notificationRepository.pendingDestination

    /** Only watched to bring the Groups tab up: the tab itself takes the code. */
    val pendingJoinCode: StateFlow<String?> = invitationLinkRepository.pendingCode

    /** Only watched to bring the Groups tab up: the tab opens the sheet and clears it. */
    val joinSheetRequested: StateFlow<Boolean> = invitationLinkRepository.joinSheetRequested

    /** The shift setup is owed, and waits for the name when the account still has none. */
    val showShiftSetup: StateFlow<Boolean> =
        combine(shiftSetupRepository.shouldShow, userRepository.userSession) { shouldShow, session ->
            shouldShow && session is UserSession.Authenticated && !session.user.needsName
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = false)

    fun destinationHandled() = notificationRepository.destinationHandled()
}
