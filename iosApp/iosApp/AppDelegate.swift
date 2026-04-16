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

        // ✅ Cold-start: app was KILLED and user tapped a notification banner
        // iOS passes the notification payload via launchOptions — handle it here
        // so the chat screen opens immediately on first compose.
        if let payload = launchOptions?[UIApplication.LaunchOptionsKey.remoteNotification] as? [AnyHashable: Any] {
            print("🧊 Cold-start notification payload:", payload)
            triggerSosIfNeeded(data: payload)
        }

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

    // 📱 APNs token → Firebase (required for iOS push delivery)
    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        print("📱 APNs token received (\(deviceToken.count) bytes)")
        Messaging.messaging().apnsToken = deviceToken
    }

    // ❌ APNs registration failed
    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        print("❌ APNs registration FAILED: \(error.localizedDescription)")
    }

    // 🔥 FCM TOKEN
    func messaging(
        _ messaging: Messaging,
        didReceiveRegistrationToken fcmToken: String?
    ) {
        print("🔥 iOS FCM TOKEN:", fcmToken ?? "")
        FcmBridge.shared.onToken(token: fcmToken ?? "")
    }

    // 📨 Data-only FCM messages (background + foreground) — REQUIRED when FirebaseAppDelegateProxyEnabled = NO
    // The backend sends chat_id, type, visitor_name etc. as DATA payload, not notification payload.
    // Without this method, iOS never delivers those data messages to the app.
    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        // Forward to Firebase so FCM analytics / token refresh works
        Messaging.messaging().appDidReceiveMessage(userInfo)

        print("📨 didReceiveRemoteNotification (data msg):", userInfo)

        // When app is in BACKGROUND: show a local notification banner so the user
        // sees an alert and can tap it to open the chat screen.
        // (The backend sends data-only push — no system banner is shown automatically.)
        if application.applicationState != .active {
            let title = (userInfo["title"] as? String) ?? "New Alert"
            let body  = (userInfo["body"]  as? String) ?? ""

            let content = UNMutableNotificationContent()
            content.title = title
            content.body  = body
            content.sound = .default
            content.userInfo = userInfo as! [String: Any]

            let request = UNNotificationRequest(
                identifier: "chat-\(userInfo["chat_id"] ?? UUID().uuidString)",
                content: content,
                trigger: nil   // deliver immediately
            )
            UNUserNotificationCenter.current().add(request) { err in
                if let err = err { print("❌ Local notif error:", err) }
            }
        }

        // Fire in-app SOS/chat navigation (foreground + background)
        triggerSosIfNeeded(data: userInfo)

        completionHandler(.newData)
    }

    // 📩 Show notification even when app is in foreground
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        let data = notification.request.content.userInfo
        print("📩 Foreground notification:", data)

        // Trigger in-app chat navigation for foreground push (same as Android onMessageReceived)
        triggerSosIfNeeded(data: data)

        completionHandler([.banner, .badge, .sound])
    }

    // 📩 Notification tap
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let data = response.notification.request.content.userInfo
        print("📲 Notification tapped:", data)

        // Trigger in-app chat navigation when user taps the notification
        triggerSosIfNeeded(data: data)

        completionHandler()
    }

    // ── Helper: extract chat_id + visitor info and fire SosNotificationHandler ──
    private func triggerSosIfNeeded(data: [AnyHashable: Any]) {
        let chatIdStr = (data["chat_id"] as? String) ?? ""
        guard let chatId = Int32(chatIdStr) else { return }

        let visitorName  = (data["visitor_name"]  as? String) ?? ""
        let visitorPhone = (data["visitor_phone"] as? String) ?? ""

        print("🚨 iOS SOS alert → chatId=\(chatId) name=\(visitorName) phone=\(visitorPhone)")
        SosNotificationHandler.shared.onSosAlertReceived(
            alertId: chatId,
            visitorName: visitorName,
            visitorPhone: visitorPhone
        )
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
