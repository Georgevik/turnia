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
