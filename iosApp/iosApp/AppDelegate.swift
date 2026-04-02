import UIKit
import ComposeApp
import PhonePePayment
import FirebaseCore
import FirebaseMessaging
import UserNotifications

class AppDelegate: NSObject,
                   UIApplicationDelegate,
                   UNUserNotificationCenterDelegate,
                   MessagingDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil
    ) -> Bool {

        // ✅ Firebase (Messaging only — no Analytics/tracking)
        FirebaseApp.configure()
        print("✅ Firebase initialized (Messaging only)")

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

    // 🔗 Deep link (PhonePe + CMP)
    func application(
        _ app: UIApplication,
        open url: URL,
        options: [UIApplication.OpenURLOptionsKey : Any] = [:]
    ) -> Bool {

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
