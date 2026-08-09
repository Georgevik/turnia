import SwiftUI
import Shared
import FirebaseCore
import GoogleSignIn

@main
struct iOSApp: App {

    // Public Google OAuth web client id (serverId) — same value as google-services.json.
    private static let webClientId =
        "570433560233-ip7aut9frd2l629vkhe34s5b6j49ojgc.apps.googleusercontent.com"

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
