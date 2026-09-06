import SwiftUI
import Shared
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

@main
struct iOSApp: App {

    // Public Google OAuth web client id (serverId) — same value as google-services.json.
    private static let webClientId =
        "570433560233-ip7aut9frd2l629vkhe34s5b6j49ojgc.apps.googleusercontent.com"

    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    init() {
        FirebaseApp.configure()
        KoinIOSKt.doInitKoin(webClientId: Self.webClientId)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    GIDSignIn.sharedInstance.handle(url)
                }
        }
    }
}
