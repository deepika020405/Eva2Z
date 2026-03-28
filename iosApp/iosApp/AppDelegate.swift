import UIKit
import ComposeApp
import PhonePePayment
import FirebaseCore
import FirebaseMessaging
import UserNotifications
import FBSDKCoreKit
import AppTrackingTransparency  // ✅ ADD THIS

class AppDelegate: NSObject,
                   UIApplicationDelegate,
                   UNUserNotificationCenterDelegate,
                   MessagingDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil
    ) -> Bool {

        // ✅ Firebase
        FirebaseApp.configure()
        print("✅ Firebase initialized")

        // ✅ Meta / Facebook SDK
        ApplicationDelegate.shared.application(
            application,
            didFinishLaunchingWithOptions: launchOptions
        )
        print("✅ Facebook SDK initialized")

        // ✅ Notifications
        UNUserNotificationCenter.current().delegate = self
        UNUserNotificationCenter.current().requestAuthorization(
            options: [.alert, .badge, .sound]
        ) { granted, _ in
            print("🔔 Notification permission:", granted)
        }

        application.registerForRemoteNotifications()

        // ✅ FCM
        Messaging.messaging().delegate = self

        return true
    }

    // ✅ ATT REQUEST — applicationDidBecomeActive mein karo
    // (Apple requirement: request after app is fully visible)
    func applicationDidBecomeActive(_ application: UIApplication) {
        if #available(iOS 14, *) {
            ATTrackingManager.requestTrackingAuthorization { status in
                switch status {
                case .authorized:
                    print("✅ ATT: Tracking authorized")
                case .denied:
                    print("❌ ATT: Tracking denied")
                case .restricted:
                    print("⚠️ ATT: Tracking restricted")
                case .notDetermined:
                    print("⏳ ATT: Not determined")
                @unknown default:
                    break
                }
            }
        }
    }

    // 🔥 FCM TOKEN
    func messaging(
        _ messaging: Messaging,
        didReceiveRegistrationToken fcmToken: String?
    ) {
        print("🔥 iOS FCM TOKEN:", fcmToken ?? "")
        FcmBridge.shared.onToken(token: fcmToken ?? "")
    }

    // 📩 Notification tap
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let data = response.notification.request.content.userInfo
        print("📲 Notification tapped:", data)
        completionHandler()
    }

    // 🔗 Deep link (Facebook + PhonePe + CMP)
    func application(
        _ app: UIApplication,
        open url: URL,
        options: [UIApplication.OpenURLOptionsKey : Any] = [:]
    ) -> Bool {

        // ✅ Facebook deep link
        if ApplicationDelegate.shared.application(
            app,
            open: url,
            options: options
        ) {
            return true
        }

        // ✅ PhonePe
        if PPPayment.checkDeeplink(url) {
            print("✅ PhonePe SDK handled deep link")
            return true
        }

        // ✅ CMP / KMP
        PaymentBridge.shared.handleDeepLink(url)
        return true
    }
}