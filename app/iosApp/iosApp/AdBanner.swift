import GoogleMobileAds
import Shared
import UIKit

/// Test or real by build configuration: set in `Configuration/Config.xcconfig`, read through `Info.plist`.
private let bannerAdUnitId = Bundle.main.object(forInfoDictionaryKey: "AdMobBannerUnitId") as! String

/// The banner the shared `AdBanner` composable embeds. Kotlin asks for the height first, to size
/// its slot, and then for the view.
final class GoogleAdBannerFactory: NSObject, AdBannerFactory {

    func height(width: Double) -> Double {
        Double(currentOrientationAnchoredAdaptiveBanner(width: CGFloat(width)).size.height)
    }

    func create(width: Double, onLoaded: @escaping () -> Void) -> UIView {
        let banner = LoadReportingBannerView(adSize: currentOrientationAnchoredAdaptiveBanner(width: CGFloat(width)))
        banner.adUnitID = bannerAdUnitId
        banner.rootViewController = rootViewController()
        banner.onLoaded = onLoaded
        banner.delegate = banner
        // Transparent until it has an ad: Compose collapses its slot until then, and an invisible
        // view takes no taps either.
        banner.alpha = 0
        banner.load(Request())
        return banner
    }
}

/// Its own delegate: `BannerView.delegate` is weak, and nothing else would keep one alive.
private final class LoadReportingBannerView: BannerView, BannerViewDelegate {
    var onLoaded: (() -> Void)?

    func bannerViewDidReceiveAd(_ bannerView: BannerView) {
        alpha = 1
        onLoaded?()
    }
}

/// Compose owns the only view controller there is; the SDKs present a tapped ad or a form over it.
func rootViewController() -> UIViewController? {
    UIApplication.shared.connectedScenes
        .compactMap { ($0 as? UIWindowScene)?.keyWindow?.rootViewController }
        .first
}
