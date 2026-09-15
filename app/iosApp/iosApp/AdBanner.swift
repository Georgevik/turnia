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

    func create(width: Double) -> UIView {
        let banner = BannerView(adSize: currentOrientationAnchoredAdaptiveBanner(width: CGFloat(width)))
        banner.adUnitID = bannerAdUnitId
        // Compose owns the only view controller there is; the SDK presents a tapped ad over it.
        banner.rootViewController = UIApplication.shared.connectedScenes
            .compactMap { ($0 as? UIWindowScene)?.keyWindow?.rootViewController }
            .first
        banner.load(Request())
        return banner
    }
}
