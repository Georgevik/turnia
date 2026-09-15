import SwiftUI
import Shared
import FirebaseAppCheck
import FirebaseCore
import FirebaseMessaging
import GoogleSignIn
import UserNotifications

/// Push needs an app delegate: FCM reports a rotated token through `MessagingDelegate`, and iOS
/// hides an incoming notification while the app is on screen unless a `UNUserNotificationCenter`
/// delegate says to show it. SwiftUI has nowhere else to put either.
///
/// Asking the user for permission is not here — it lives in the shared UI, so it happens once they
/// are signed in rather than the moment the app first launches.
class AppDelegate: NSObject, UIApplicationDelegate, MessagingDelegate,
                   UNUserNotificationCenterDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        Messaging.messaging().delegate = self
        UNUserNotificationCenter.current().delegate = self
        return true
    }

    /// Handed over by hand: Firebase's swizzling hooks whatever delegate exists at `configure()`,
    /// which runs in `iOSApp.init` before SwiftUI installs this one, so it never sees the token and
    /// FCM refuses to issue one of its own.
    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        Messaging.messaging().apnsToken = deviceToken
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        PushTokenBridgeKt.onPushTokenRefreshed()
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound])
    }

    /// The user tapped a notification. Also called on a cold start, once this delegate is set,
    /// which is why the destination is held as state until the UI is there to act on it.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        // Only the string entries: `aps` is a dictionary, and the shared code reads none of it.
        let data = userInfo.reduce(into: [String: String]()) { result, entry in
            if let key = entry.key as? String, let value = entry.value as? String {
                result[key] = value
            }
        }

        PushNavigationBridgeKt.onPushOpened(data: data)
        completionHandler()
    }
}

/// App Check's attestation. A debug build — the simulator above all — cannot attest, so it sends a
/// debug token instead: the SDK prints it on first launch, and it goes into the console (App Check →
/// Apps → Manage debug tokens) once per device. A release build uses App Attest.
final class TurniaAppCheckProviderFactory: NSObject, AppCheckProviderFactory {
    func createProvider(with app: FirebaseApp) -> AppCheckProvider? {
        #if DEBUG
        return AppCheckDebugProvider(app: app)
        #else
        return AppAttestProvider(app: app)
        #endif
    }
}

@main
struct iOSApp: App {

    // Public Google OAuth web client id (serverId) — same value as google-services.json.
    private static let webClientId =
        "570433560233-ip7aut9frd2l629vkhe34s5b6j49ojgc.apps.googleusercontent.com"

    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    #if DEBUG
    private static let isDebug = true
    #else
    private static let isDebug = false
    #endif

    /// Made-up data instead of Firebase, for store screenshots: launch a debug build with
    /// `-TurniaDemo` (`xcrun simctl launch booted com.geoviksoft.turnia.Turnia -TurniaDemo`).
    #if DEBUG
    private static let demo = ProcessInfo.processInfo.arguments.contains("-TurniaDemo")
    #else
    private static let demo = false
    #endif

    init() {
        // Before configure(): the factory is read when Firebase starts, and a call made without it
        // goes out with no App Check token.
        AppCheck.setAppCheckProviderFactory(TurniaAppCheckProviderFactory())
        FirebaseApp.configure()
        KoinIOSKt.doInitKoin(webClientId: Self.webClientId, isDebug: Self.isDebug, demo: Self.demo)
        AdBannerBridgeKt.registerAdBannerFactory(factory: GoogleAdBannerFactory())
        AdBannerBridgeKt.registerAdConsentPlatform(platform: GoogleAdConsentPlatform())
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    if GIDSignIn.sharedInstance.handle(url) { return }
                    // The invitation link: SwiftUI delivers both the custom scheme and the https
                    // Universal Link through this same handler.
                    InvitationLinkBridgeKt.onLinkOpened(link: url.absoluteString)
                }
        }
    }
}
