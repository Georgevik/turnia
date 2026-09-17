package com.geoviksoft.turnia.e2e.infra

import com.geoviksoft.turnia.core.domain.analytics.Analytics
import com.geoviksoft.turnia.core.domain.analytics.AnalyticsEvent
import com.geoviksoft.turnia.core.domain.model.FeatureFlags
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.domain.repository.AppConfigRepository
import com.geoviksoft.turnia.core.domain.repository.FcmDelegate
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.toSuccess
import com.geoviksoft.turnia.ui.system.ads.AdConsent
import com.geoviksoft.turnia.ui.system.ads.AdConsentStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.dsl.module

/**
 * What has no emulator: Remote Config, FCM, Analytics and Google's consent SDK — and onboarding,
 * which [E2eRule] decides per test.
 */
internal val e2eModule = module {
    single<AppConfigRepository> { FixedAppConfigRepository }
    single<FcmDelegate> { NoFcmDelegate }
    single<Analytics> { NoAnalytics }
    single {
        AdConsent(
            granted = AdConsentStatus(
                canRequestAds = true,
                privacyOptionsRequired = false
            )
        )
    }
}

internal object FixedAppConfigRepository : AppConfigRepository {
    var onboardingSeen = true
    private val flags = FeatureFlags(
        minActionsToEnableAds = -1,
        invitationCodeLength = 6,
        enableSubscription = false,
        supportEmail = "support@turnia.club",
    )
    override val featureFlags: StateFlow<FeatureFlags> = MutableStateFlow(flags).asStateFlow()
    override suspend fun refreshFeatureFlags(): FeatureFlags = flags
    override suspend fun isOnboardingSeen(): Boolean = onboardingSeen

    override suspend fun setOnboardingSeen(seen: Boolean) {
        onboardingSeen = seen
    }
}

private object NoFcmDelegate : FcmDelegate {
    override val notificationsEnabled: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()
    override suspend fun registerFcmToken(uid: UserId) = Unit
    override suspend fun unregisterFcmToken(uid: UserId) = Unit
    override suspend fun setNotificationsEnabled(
        uid: UserId,
        enabled: Boolean
    ): Outcome<Unit, Unit> =
        Unit.toSuccess()
}

private object NoAnalytics : Analytics {
    override fun log(event: AnalyticsEvent) = Unit
    override fun setUser(userId: UserId?) = Unit
}
