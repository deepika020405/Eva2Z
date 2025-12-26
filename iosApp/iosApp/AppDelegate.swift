import UIKit
import ComposeApp
import PhonePePayment
import FirebaseCore  // ✅ ADD THIS

class AppDelegate: NSObject, UIApplicationDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil
    ) -> Bool {

        // ✅ INITIALIZE FIREBASE
        FirebaseApp.configure()
        print("✅ Firebase initialized")

        print("✅ AppDelegate initialized")
        return true
    }

    func application(
        _ app: UIApplication,
        open url: URL,
        options: [UIApplication.OpenURLOptionsKey : Any] = [:]
    ) -> Bool {
        print("📲 AppDelegate: Deep link opened: \(url.absoluteString)")

        let handled = PPPayment.checkDeeplink(url)
        if handled {
            print("✅ PhonePe SDK handled deep link")
            return true
        }

        PaymentBridge.shared.handleDeepLink(url)
        return true
    }
}