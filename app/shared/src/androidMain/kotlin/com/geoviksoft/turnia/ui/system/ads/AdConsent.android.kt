package com.geoviksoft.turnia.ui.system.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlin.concurrent.thread

@Composable
internal actual fun rememberAdConsentPlatform(): AdConsentPlatform? {
    val activity = LocalContext.current.findActivity() ?: return null
    return remember(activity) { AndroidAdConsentPlatform(activity) }
}

private class AndroidAdConsentPlatform(private val activity: Activity) : AdConsentPlatform {

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(activity)

    override fun status() = AdConsentStatus(
        canRequestAds = consentInformation.canRequestAds(),
        privacyOptionsRequired = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED,
    )

    override fun gather(onDone: (AdConsentStatus?) -> Unit) {
        consentInformation.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                    onDone(if (error == null) status() else null)
                }
            },
            { onDone(null) },
        )
    }

    override fun showPrivacyOptions(onDone: (AdConsentStatus) -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { onDone(status()) }
    }

    override fun startAds() {
        val context = activity.applicationContext
        // Off the main thread, as Google asks: initialisation is slow and would drop frames.
        thread { MobileAds.initialize(context) }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
