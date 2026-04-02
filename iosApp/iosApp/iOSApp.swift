// iosApp/iOSApp.swift
import SwiftUI
import ComposeApp

@main
struct iOSApp: App {

    // ✅ Register AppDelegate
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    init() {
        // ✅ Initialize PaymentBridge on app launch
        _ = PaymentBridge.shared
        print("🚀 Eva2z Connect iOS - PaymentBridge initialized")
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    // ✅ Handle deep link
                    PaymentBridge.shared.handleDeepLink(url)
                }
        }
    }
}
