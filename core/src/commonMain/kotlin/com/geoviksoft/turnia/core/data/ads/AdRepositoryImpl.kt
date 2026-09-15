package com.geoviksoft.turnia.core.data.ads

import com.geoviksoft.turnia.core.domain.model.Membership
import com.geoviksoft.turnia.core.domain.model.UserSession
import com.geoviksoft.turnia.core.domain.repository.AdRepository
import com.geoviksoft.turnia.core.domain.repository.AppConfigRepository
import com.geoviksoft.turnia.core.domain.repository.UserRepository
import com.geoviksoft.turnia.core.system.onFailure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class AdRepositoryImpl(
    private val userRepository: UserRepository,
    appConfigRepository: AppConfigRepository,
    private val scope: CoroutineScope,
) : AdRepository {

    /**
     * Counted in memory: only the flag on the profile is stored, so a session that ends short of the
     * threshold starts again from zero. The write happens once, when the threshold is reached.
     */
    private var actions = 0
    private var writing = false

    override val bannerVisible: Flow<Boolean> = combine(
        userRepository.userSession,
        appConfigRepository.featureFlags,
    ) { session, flags ->
        val user = (session as? UserSession.Authenticated)?.user
        flags.enableAds && user?.membership == Membership.FREE && user.showAds
    }.distinctUntilChanged()

    override fun actionPerformed() {
        val user = userRepository.loggedUser ?: return
        if (user.showAds || writing) return
        if (++actions < ACTIONS_BEFORE_BANNER) return

        writing = true
        scope.launch {
            // A failed write leaves the flag unset, and the next action tries again.
            userRepository.enableShowAds().onFailure { writing = false }
        }
    }

    private companion object {
        const val ACTIONS_BEFORE_BANNER = 10
    }
}
