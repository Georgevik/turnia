package com.geoviksoft.turnia.ui.root

import androidx.lifecycle.ViewModel
import com.geoviksoft.turnia.core.domain.repository.InvitationLinkRepository
import com.geoviksoft.turnia.core.domain.repository.UserRepository
import kotlinx.coroutines.flow.StateFlow

class RootViewModel(
    userRepository: UserRepository,
    invitationLinkRepository: InvitationLinkRepository,
) : ViewModel() {

    val userSession = userRepository.userSession

    /** Only watched to uncover Main: the Groups tab inside it is what takes the code. */
    val pendingJoinCode: StateFlow<String?> = invitationLinkRepository.pendingCode
}
