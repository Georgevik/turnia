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
 * Consent for ads, asked for when the banner first becomes due rather than at launch: a new user
 * sees no ad for their first actions, and has nothing to consent to until then. Outside the regions
 * that need consent the SDK answers straight away that ads can be requested.
 */
class AdConsent {

    private val _status = MutableStateFlow<AdConsentStatus?>(null)
    val status: StateFlow<AdConsentStatus?> = _status.asStateFlow()

    private var gathering = false
    private var adsStarted = false

    /** Reads the answer stored on the device, which is enough to draw a banner while [gather] runs. */
    fun load(platform: AdConsentPlatform) {
        if (_status.value == null) update(platform, platform.status())
    }

    /** Once per launch: a failure is tried again on the next one. */
    fun gather(platform: AdConsentPlatform) {
        load(platform)
        if (gathering) return
        gathering = true

        // On failure the answer stored from an earlier launch still stands.
        platform.gather { status -> update(platform, status ?: platform.status()) }
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
