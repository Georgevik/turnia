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

    fun create(width: Double): UIView
}

internal var adBannerFactory: AdBannerFactory? = null
    private set

fun registerAdBannerFactory(factory: AdBannerFactory) {
    adBannerFactory = factory
}
