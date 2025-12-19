import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    // ✅ Register AppDelegate
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}