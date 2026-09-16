package com.geoviksoft.turnia.core.data.config

import com.geoviksoft.turnia.core.domain.model.FeatureFlags
import com.geoviksoft.turnia.core.system.BuildInfo
import dev.gitlive.firebase.remoteconfig.FirebaseRemoteConfig
import dev.gitlive.firebase.remoteconfig.get
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

class RemoteConfigService(
    private val remoteConfig: FirebaseRemoteConfig,
    private val buildInfo: BuildInfo,
) {

    init {
        // Blocking, and before anything can read: until the defaults are set Remote Config answers
        // 0 and false for every key, so the first flags would turn ads on and invitation codes empty.
        // iOS sets them synchronously; Android's task resumes on the thread that completes it, so
        // this cannot deadlock the main thread.
        runBlocking { setDefaults() }
    }

    suspend fun init() {
        remoteConfig.settings {
            minimumFetchInterval = if (buildInfo.isDebug) Duration.ZERO else 1.days
        }
        remoteConfig.ensureInitialized()
    }

    suspend fun refresh() {
        remoteConfig.fetchAndActivate()
    }

    fun getFlags(): FeatureFlags {
        return FeatureFlags(
            // GitLive's get has no Int: anything but Boolean, Double, Long or String throws.
            minActionsToEnableAds = remoteConfig.getSafe<Long>(RemoteKey.MIN_ADS_ACTION, 0).toInt(),
            invitationCodeLength = remoteConfig.getSafe<Long>(RemoteKey.INVITATION_CODE_LENGTH, 6)
                .toInt(),
            enableSubscription = remoteConfig.getSafe<Boolean>(
                RemoteKey.ENABLE_SUBSCRIPTION, false
            ),
            supportEmail = remoteConfig.getSafe<String>(
                RemoteKey.SUPPORT_EMAIL, DEFAULT_SUPPORT_EMAIL
            ).ifBlank { DEFAULT_SUPPORT_EMAIL },
        )
    }

    private inline fun <reified T> FirebaseRemoteConfig.getSafe(key: RemoteKey, default: T): T =
        try {
            remoteConfig.get<T>(key.value)
        } catch (e: Exception) {
            default
        }


    private suspend fun setDefaults() {
        remoteConfig.setDefaults(
            RemoteKey.MIN_ADS_ACTION.value to 0,
            RemoteKey.INVITATION_CODE_LENGTH.value to 6,
            RemoteKey.ENABLE_SUBSCRIPTION.value to false,
            RemoteKey.SUPPORT_EMAIL.value to DEFAULT_SUPPORT_EMAIL,
        )
    }

    companion object {
        private const val DEFAULT_SUPPORT_EMAIL = "geoviksoft@gmail.com"
    }

}

enum class RemoteKey(val value: String) {
    MIN_ADS_ACTION("minAdsAction"), INVITATION_CODE_LENGTH("invitation_code_length"), ENABLE_SUBSCRIPTION(
        "enableSubscription"
    ),
    SUPPORT_EMAIL("supportEmail"),
}
