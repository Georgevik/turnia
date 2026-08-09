import Foundation
import UIKit
import Shared
import GoogleSignIn

/// iOS implementation of the shared `AuthProvider` interface.
///
/// GoogleSignIn is a Swift/SPM SDK (not reachable from Kotlin/Native), so the
/// sign-in flow lives here in Swift and is injected into Koin at startup
/// (see `iOSApp.swift`). Kotlin's `suspend fun getGoogleToken()` is exported to
/// Swift as a completion-handler method returning a `GoogleSignInResult`.
final class IOSAuthProvider: AuthProvider {

    private static let logTag = "IOSAuthProvider"

    func getGoogleToken(completionHandler: @escaping (GoogleSignInResult?, Error?) -> Void) {
        DispatchQueue.main.async {
            guard let presenter = Self.topViewController() else {
                Logger.shared.e(tag: Self.logTag, message: "No presenting view controller", throwable: nil)
                completionHandler(GoogleSignInResultFailure(error: .unknown), nil)
                return
            }
            GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { result, error in
                if let error {
                    if let signInError = error as? GIDSignInError, signInError.code == .canceled {
                        Logger.shared.i(tag: Self.logTag, message: "Google sign-in cancelled by the user")
                        completionHandler(GoogleSignInResultFailure(error: .cancelled), nil)
                    } else {
                        Logger.shared.e(tag: Self.logTag, message: "Google sign-in failed: \(error.localizedDescription)", throwable: nil)
                        completionHandler(GoogleSignInResultFailure(error: .unknown), nil)
                    }
                    return
                }
                guard let idToken = result?.user.idToken?.tokenString else {
                    Logger.shared.e(tag: Self.logTag, message: "Missing Google ID token", throwable: nil)
                    completionHandler(GoogleSignInResultFailure(error: .unknown), nil)
                    return
                }
                let accessToken = result?.user.accessToken.tokenString
                Logger.shared.i(tag: Self.logTag, message: "Google sign-in succeeded")
                completionHandler(
                    GoogleSignInResultSuccess(
                        token: GoogleSignInToken(idToken: idToken, accessToken: accessToken)
                    ),
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
