import UIKit
import ComposeApp

// REMOVE @UIApplicationMain
class AppDelegate: NSObject, UIApplicationDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil
    ) -> Bool {
        print("✅ App Delegate initialized")
        return true
    }

    // PhonePe callback handler
    func application(
        _ application: UIApplication,
        open url: URL,
        options: [UIApplication.OpenURLOptionsKey : Any] = [:]
    ) -> Bool {

        print("📲 PhonePe Callback URL = \(url)")

        let components = URLComponents(url: url, resolvingAgainstBaseURL: false)

        let status = components?.queryItems?
            .first(where: { $0.name == "status" })?.value

        let txnId = components?.queryItems?
            .first(where: { $0.name == "transactionId" })?.value

        let success = status == "SUCCESS"
//
//         // 🔥 KMP bridge call
//         PaymentBus.shared.onResult(
//             success: success,
//             txnId: txnId,
//             reason: status
//         )

        return true
    }
}