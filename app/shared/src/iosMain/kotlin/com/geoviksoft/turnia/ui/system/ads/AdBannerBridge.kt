package com.geoviksoft.turnia.ui.system.ads

import platform.UIKit.UIView

/**
 * Builds the Google Mobile Ads banner. The SDK is a Swift package Kotlin cannot import, so Swift
 * implements this and hands it over from `iOSApp.init` through
 * `AdBannerBridgeKt.registerAdBannerFactory(factory:)`. Until it does, no banner is drawn.
 */
interface AdBannerFactory {
    /** The ad's height, in points, for a banner as wide as [width]. */
    fun height(width: Double): Double

    /** [onLoaded] is called on the main thread every time an ad arrives. */
    fun create(width: Double, onLoaded: () -> Unit): UIView
}

internal var adBannerFactory: AdBannerFactory? = null
    private set

internal var adConsentPlatform: AdConsentPlatform? = null
    private set

fun registerAdBannerFactory(factory: AdBannerFactory) {
    adBannerFactory = factory
}

/** The User Messaging Platform is a Swift package too, handed over the same way. */
fun registerAdConsentPlatform(platform: AdConsentPlatform) {
    adConsentPlatform = platform
}
