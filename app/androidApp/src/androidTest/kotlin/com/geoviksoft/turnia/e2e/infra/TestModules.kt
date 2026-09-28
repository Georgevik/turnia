package com.geoviksoft.turnia.e2e.infra

import com.geoviksoft.turnia.core.data.config.SharePromptRepositoryImpl
import com.geoviksoft.turnia.core.data.config.mappers.SharePromptMilestonesMapper
import com.geoviksoft.turnia.core.domain.analytics.Analytics
import com.geoviksoft.turnia.core.domain.analytics.AnalyticsEvent
import com.geoviksoft.turnia.core.domain.analytics.AnalyticsUserProperty
import com.geoviksoft.turnia.core.domain.model.EventKind
import com.geoviksoft.turnia.core.domain.model.FeatureFlags
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.domain.repository.AppConfigRepository
import com.geoviksoft.turnia.core.domain.repository.FcmDelegate
import com.geoviksoft.turnia.core.domain.repository.SharePromptRepository
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.toSuccess
import com.geoviksoft.turnia.ui.system.TextSharer
import com.geoviksoft.turnia.ui.system.ads.AdConsent
import com.geoviksoft.turnia.ui.system.ads.AdConsentStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.dsl.module
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * What has no emulator: Remote Config, FCM, Analytics and Google's consent SDK — and onboarding,
 * which [E2eRule] decides per test.
 */
internal val e2eModule = module {
    single<AppConfigRepository> { FixedAppConfigRepository }
    single<FcmDelegate> { NoFcmDelegate }
    single<Analytics> { RecordingAnalytics }
    single<TextSharer> { RecordingTextSharer }
    single<SharePromptRepository> {
        CountingSharePromptRepository(SharePromptRepositoryImpl(get(), get(), get()))
    }
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
    var shiftSetupSettled = true
    private val flags = MutableStateFlow(
        FeatureFlags(
            minActionsToEnableAds = -1,
            invitationCodeLength = 6,
            enableSubscription = false,
            supportEmail = "support@turnia.club",
        )
    )
    override val featureFlags: StateFlow<FeatureFlags> = flags.asStateFlow()
    override suspend fun refreshFeatureFlags(): FeatureFlags = flags.value

    /** [milestones] as the console would hold them, read through the app's own parser. */
    fun sharePrompt(enabled: Boolean, milestones: String) = flags.update {
        it.copy(
            sharePromptEnabled = enabled,
            sharePromptMilestones = SharePromptMilestonesMapper().map(milestones),
        )
    }
    override suspend fun isOnboardingSeen(): Boolean = onboardingSeen

    override suspend fun setOnboardingSeen(seen: Boolean) {
        onboardingSeen = seen
    }

    override suspend fun isShiftSetupSettled(): Boolean = shiftSetupSettled

    override suspend fun setShiftSetupSettled(settled: Boolean) {
        shiftSetupSettled = settled
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

/** Keeps what the app reports, so a test can check the events without a console. */
internal object RecordingAnalytics : Analytics {
    private val logged = CopyOnWriteArrayList<AnalyticsEvent>()

    val events: List<AnalyticsEvent> get() = logged.toList()

    fun named(name: String): List<AnalyticsEvent> = events.filter { it.name == name }

    private val properties = ConcurrentHashMap<String, String>()

    /** The last value each user property was set to, as the console would hold it. */
    fun property(name: String): String? = properties[name]

    override fun log(event: AnalyticsEvent) {
        logged += event
    }

    override fun setUserProperty(property: AnalyticsUserProperty) {
        properties[property.name] = property.value
    }

    override fun setUser(userId: UserId?) = Unit
}

/** Stands in for the system's share sheet, which a test cannot reach, and keeps what was shared. */
internal object RecordingTextSharer : TextSharer {
    private val sharedTexts = CopyOnWriteArrayList<String>()

    val shared: List<String> get() = sharedTexts.toList()

    override fun copy(text: String) = Unit

    override fun share(text: String) {
        sharedTexts += text
    }
}

/**
 * The app's own repository, counting each add once it has decided on a prompt. A test asserting
 * that no prompt shows waits for that count first, so a late prompt fails it instead of arriving
 * after the test has looked.
 */
internal class CountingSharePromptRepository(
    private val delegate: SharePromptRepository,
) : SharePromptRepository by delegate {

    override suspend fun eventAdded(kind: EventKind) {
        delegate.eventAdded(kind)
        counted.incrementAndGet()
    }

    companion object {
        private val counted = AtomicInteger()

        val eventsCounted: Int get() = counted.get()
    }
}
