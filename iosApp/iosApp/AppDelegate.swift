// iosApp/AppDelegate.swift
import UIKit
import ComposeApp
import PhonePePayment

class AppDelegate: NSObject, UIApplicationDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil
    ) -> Bool {
        print("✅ AppDelegate initialized")
        return true
    }

    // ✅ Handle deep link callback
    func application(
        _ app: UIApplication,
        open url: URL,
        options: [UIApplication.OpenURLOptionsKey : Any] = [:]
    ) -> Bool {
        print("📲 AppDelegate: Deep link opened: \(url.absoluteString)")

        // ✅ Let PhonePe SDK handle it first
        let handled = PPPayment.checkDeeplink(url)
        if handled {
            print("✅ PhonePe SDK handled deep link")
            return true
        }

        // ✅ Otherwise, pass to PaymentBridge
        PaymentBridge.shared.handleDeepLink(url)
        return true
    }
}