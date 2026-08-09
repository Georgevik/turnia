import Foundation
import UIKit
import Shared
import GoogleSignIn

/// iOS implementation of the shared `AuthProvider` interface.
///
/// GoogleSignIn is a Swift/SPM SDK (not reachable from Kotlin/Native), so the
/// sign-in flow lives here in Swift and is injected into Koin at startup
/// (see `iOSApp.swift`). Kotlin's `suspend fun getGoogleToken()` is exported to
/// Swift as a completion-handler method.
final class IOSAuthProvider: AuthProvider {

    func getGoogleToken(completionHandler: @escaping (GoogleSignInToken?, Error?) -> Void) {
        DispatchQueue.main.async {
            guard let presenter = Self.topViewController() else {
                completionHandler(nil, nil)
                return
            }
            GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { result, error in
                if let error {
                    completionHandler(nil, error)
                    return
                }
                guard let idToken = result?.user.idToken?.tokenString else {
                    completionHandler(nil, nil)
                    return
                }
                let accessToken = result?.user.accessToken.tokenString
                completionHandler(
                    GoogleSignInToken(idToken: idToken, accessToken: accessToken),
                    nil
                )
            }
        }
    }

    private static func topViewController(
        _ base: UIViewController? = UIApplication.shared.connectedScenes
            .compactMap { ($0 as? UIWindowScene)?.keyWindow }
            .first?.rootViewController
    ) -> UIViewController? {
        if let nav = base as? UINavigationController {
            return topViewController(nav.visibleViewController)
        }
        if let tab = base as? UITabBarController {
            return topViewController(tab.selectedViewController)
        }
        if let presented = base?.presentedViewController {
            return topViewController(presented)
        }
        return base
    }
}
