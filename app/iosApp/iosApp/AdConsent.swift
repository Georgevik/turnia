import GoogleMobileAds
import Shared
import UserMessagingPlatform

/// The consent message the shared `AdConsent` asks for before the first banner.
final class GoogleAdConsentPlatform: NSObject, AdConsentPlatform {

    func status() -> AdConsentStatus {
        AdConsentStatus(
            canRequestAds: ConsentInformation.shared.canRequestAds,
            privacyOptionsRequired: ConsentInformation.shared.privacyOptionsRequirementStatus == .required
        )
    }

    func gather(onDone: @escaping (AdConsentStatus?) -> Void) {
        ConsentInformation.shared.requestConsentInfoUpdate(with: RequestParameters()) { [weak self] error in
            guard let self, error == nil else { return onDone(nil) }
            ConsentForm.loadAndPresentIfRequired(from: rootViewController()) { error in
                onDone(error == nil ? self.status() : nil)
            }
        }
    }

    func showPrivacyOptions(onDone: @escaping (AdConsentStatus) -> Void) {
        ConsentForm.presentPrivacyOptionsForm(from: rootViewController()) { [weak self] _ in
            guard let self else { return }
            onDone(self.status())
        }
    }

    func startAds() {
        MobileAds.shared.start()
    }
}
