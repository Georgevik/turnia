package com.geoviksoft.turnia.ui.root

import androidx.lifecycle.ViewModel
import com.geoviksoft.turnia.core.domain.analytics.Analytics
import com.geoviksoft.turnia.core.domain.analytics.AnalyticsUserProperty
import com.geoviksoft.turnia.core.domain.repository.InvitationLinkRepository
import com.geoviksoft.turnia.core.domain.repository.UserRepository
import com.geoviksoft.turnia.ui.system.currentAppLanguage
import kotlinx.coroutines.flow.StateFlow

class RootViewModel(
    userRepository: UserRepository,
    invitationLinkRepository: InvitationLinkRepository,
    analytics: Analytics,
) : ViewModel() {

    init {
        // The OS keeps the pick, and Settings can change it with the app closed: read it each launch.
        analytics.setUserProperty(AnalyticsUserProperty.AppLanguage(currentAppLanguage().tag))
    }

    val userSession = userRepository.userSession

    /** Only watched to uncover Main: the Groups tab inside it is what takes the code. */
    val pendingJoinCode: StateFlow<String?> = invitationLinkRepository.pendingCode
}
