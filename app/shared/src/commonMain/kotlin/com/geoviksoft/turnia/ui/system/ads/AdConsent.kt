package com.geoviksoft.turnia.ui.system.ads

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AdConsentStatus(
    val canRequestAds: Boolean,
    /** The user's region lets them change their answer later, so Preferences must offer it. */
    val privacyOptionsRequired: Boolean,
)

/**
 * Google's User Messaging Platform on each platform. Its SDK keeps the user's answer on the device,
 * so [status] is the one from the last launch until [gather] has asked again.
 */
interface AdConsentPlatform {
    fun status(): AdConsentStatus

    /** Refreshes the consent information and shows the form if the region needs one; null if that failed. */
    fun gather(onDone: (AdConsentStatus?) -> Unit)

    fun showPrivacyOptions(onDone: (AdConsentStatus) -> Unit)

    /** Starts the Mobile Ads SDK, which Google asks not to do before ads may be requested. */
    fun startAds()
}

/** Null where the platform has not provided one: no consent can be asked for, and no banner is drawn. */
@Composable
internal expect fun rememberAdConsentPlatform(): AdConsentPlatform?

/**
 * Consent for ads, asked for on the splash screen: Turnia is paid for by its ads, so the app is not
 * usable until the user has answered the consent message. Outside the regions that need consent the
 * SDK answers straight away that ads can be requested.
 */
class AdConsent(granted: AdConsentStatus? = null) {

    private val _status = MutableStateFlow(granted)
    val status: StateFlow<AdConsentStatus?> = _status.asStateFlow()

    private val _gathering = MutableStateFlow(false)
    val gathering: StateFlow<Boolean> = _gathering.asStateFlow()

    // A status handed in up front (the E2E tests, where UMP must not reach the network) is final.
    private var gathered = granted?.canRequestAds == true
    private var adsStarted = false

    /** Reads the answer stored on the device, which is enough to draw a banner while [gather] runs. */
    fun load(platform: AdConsentPlatform) {
        if (_status.value == null) update(platform, platform.status())
    }

    /**
     * Asks once per launch. An attempt that leaves ads unrequestable — the form failed to load, or was
     * closed without an answer — does not count, so calling this again retries.
     */
    fun gather(platform: AdConsentPlatform) {
        load(platform)
        if (gathered || _gathering.value) return
        _gathering.value = true

        // On failure the answer stored from an earlier launch still stands.
        platform.gather { result ->
            val status = result ?: platform.status()
            gathered = status.canRequestAds
            update(platform, status)
            _gathering.value = false
        }
    }

    fun showPrivacyOptions(platform: AdConsentPlatform) {
        platform.showPrivacyOptions { update(platform, it) }
    }

    private fun update(platform: AdConsentPlatform, status: AdConsentStatus) {
        _status.value = status
        if (status.canRequestAds && !adsStarted) {
            adsStarted = true
            platform.startAds()
        }
    }
}
