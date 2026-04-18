import UIKit
import AVFoundation
import ComposeApp
import PhonePePayment
import FirebaseCore
import FirebaseMessaging
import UserNotifications

class AppDelegate: NSObject,
                   UIApplicationDelegate,
                   UNUserNotificationCenterDelegate,
                   MessagingDelegate {

    // isCallActive is now read from WebRtcBridge.shared so it stays in sync
    // when the owner ends the call from the app side (no FCM comes in that case).
    private let callNotifId = "incoming-call"

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil
    ) -> Bool {

        // Firebase (Messaging only — no Analytics/tracking)
        FirebaseApp.configure()
        print("✅ Firebase initialized (Messaging only)")

        // Notification permission — request alert + badge + sound
        UNUserNotificationCenter.current().delegate = self
        UNUserNotificationCenter.current().requestAuthorization(
            options: [.alert, .badge, .sound]
        ) { granted, error in
            if let error = error {
                print("❌ Notification permission error: \(error.localizedDescription)")
            } else {
                print("🔔 Notification permission granted: \(granted)")
            }
            DispatchQueue.main.async {
                if granted {
                    application.registerForRemoteNotifications()
                }
            }
        }

        // Microphone permission — request upfront so it's ready for calls
        AVAudioSession.sharedInstance().requestRecordPermission { granted in
            print("🎤 Microphone permission granted: \(granted)")
        }

        // FCM
        Messaging.messaging().delegate = self

        // Register WebRTC bridge so VoiceCallManager.ios.kt can delegate to it
        WebRtcBridgeHolder.shared.delegate = WebRtcBridge.shared
        print("✅ WebRtcBridge registered")

        // Cold-start: app killed and user tapped a notification banner
        if let payload = launchOptions?[UIApplication.LaunchOptionsKey.remoteNotification] as? [AnyHashable: Any] {
            print("🧊 Cold-start notification payload:", payload)
            handleCallIfNeeded(data: payload)
            triggerSosIfNeeded(data: payload)
        }

        return true
    }

    // MARK: - APNs Token

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        print("📱 APNs token received (\(deviceToken.count) bytes)")
        Messaging.messaging().apnsToken = deviceToken
    }

    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        print("❌ APNs registration FAILED: \(error.localizedDescription)")
    }

    // MARK: - FCM Token

    func messaging(
        _ messaging: Messaging,
        didReceiveRegistrationToken fcmToken: String?
    ) {
        print("🔥 iOS FCM TOKEN:", fcmToken ?? "")
        FcmBridge.shared.onToken(token: fcmToken ?? "")
    }

    // MARK: - Data-only FCM (background + foreground)
    // Required when FirebaseAppDelegateProxyEnabled = NO.
    // Backend sends call/SOS events as DATA payloads, not notification payloads.

    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        Messaging.messaging().appDidReceiveMessage(userInfo)
        print("📨 didReceiveRemoteNotification:", userInfo)

        let type = userInfo["type"] as? String ?? ""

        // Call-related FCM types — handle exclusively (no generic banner)
        if ["incoming_call", "call_ended", "call_accepted"].contains(type) {
            handleCallIfNeeded(data: userInfo)
            completionHandler(.newData)
            return
        }

        // Generic SOS/chat — show local banner when backgrounded
        if application.applicationState != .active {
            let title = (userInfo["title"] as? String) ?? "New Alert"
            let body  = (userInfo["body"]  as? String) ?? ""

            let content = UNMutableNotificationContent()
            content.title   = title
            content.body    = body
            content.sound   = .default
            content.userInfo = userInfo as! [String: Any]

            let request = UNNotificationRequest(
                identifier: "chat-\(userInfo["chat_id"] ?? UUID().uuidString)",
                content: content,
                trigger: nil
            )
            UNUserNotificationCenter.current().add(request) { err in
                if let err = err { print("❌ Local notif error:", err) }
            }
        }

        triggerSosIfNeeded(data: userInfo)
        completionHandler(.newData)
    }

    // MARK: - Foreground notification display

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        let data = notification.request.content.userInfo
        let type = data["type"] as? String ?? ""
        print("📩 Foreground notification (type=\(type)):", data)

        if ["incoming_call", "call_ended", "call_accepted"].contains(type) {
            // Call events are handled by the Compose UI — suppress system banner
            handleCallIfNeeded(data: data)
            completionHandler([])
            return
        }

        triggerSosIfNeeded(data: data)
        completionHandler([.banner, .badge, .sound])
    }

    // MARK: - Notification tap

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let data = response.notification.request.content.userInfo
        let type = data["type"] as? String ?? ""
        print("📲 Notification tapped (type=\(type)):", data)

        if ["incoming_call", "call_ended", "call_accepted"].contains(type) {
            handleCallIfNeeded(data: data)
        } else {
            triggerSosIfNeeded(data: data)
        }

        completionHandler()
    }

    // MARK: - Call handling

    private func handleCallIfNeeded(data: [AnyHashable: Any]) {
        let type         = data["type"]          as? String ?? ""
        let visitorName  = data["visitor_name"]  as? String ?? ""
        let visitorPhone = data["visitor_phone"] as? String ?? ""
        let roomId       = data["room_id"]       as? String ?? ""
        let chatIdStr    = data["chat_id"]       as? String ?? ""
        let chatIdInt    = Int32(chatIdStr) ?? -1

        switch type {

        case "incoming_call":
            guard !WebRtcBridge.shared.isCallActive else {
                print("⚠️ incoming_call ignored — call already active")
                return
            }
            print("📞 iOS INCOMING CALL — visitor=\(visitorName) phone=\(visitorPhone) room=\(roomId)")

            // Notify Kotlin UI layer
            let chatId: KotlinInt? = chatIdInt > 0 ? KotlinInt(value: chatIdInt) : nil
            SosNotificationHandler.shared.onIncomingCallReceived(
                visitorName:  visitorName,
                visitorPhone: visitorPhone,
                roomId:       roomId,
                chatId:       chatId
            )
            // Show ringtone notification for when app is backgrounded/killed
            showCallNotification(visitorName: visitorName, visitorPhone: visitorPhone)

        case "call_ended":
            print("📵 iOS CALL ENDED")
            dismissCallNotification()
            SosNotificationHandler.shared.onCallEnded()

        case "call_accepted":
            print("📱 iOS CALL ACCEPTED ELSEWHERE")
            dismissCallNotification()
            SosNotificationHandler.shared.onCallAcceptedElsewhere()

        default:
            break
        }
    }

    private func showCallNotification(visitorName: String, visitorPhone: String) {
        let content         = UNMutableNotificationContent()
        content.title       = visitorName.isEmpty ? "Incoming Call" : visitorName
        content.body        = visitorPhone.isEmpty ? "Visitor is calling..." : visitorPhone
        content.sound       = .default
        content.userInfo    = ["type": "incoming_call"]

        let request = UNNotificationRequest(
            identifier: callNotifId,
            content: content,
            trigger: nil
        )
        UNUserNotificationCenter.current().add(request) { err in
            if let err = err { print("❌ Call notification error:", err) }
        }
    }

    func dismissCallNotification() {
        UNUserNotificationCenter.current()
            .removePendingNotificationRequests(withIdentifiers: [callNotifId])
        UNUserNotificationCenter.current()
            .removeDeliveredNotifications(withIdentifiers: [callNotifId])
    }

    // MARK: - SOS helper

    private func triggerSosIfNeeded(data: [AnyHashable: Any]) {
        let chatIdStr = (data["chat_id"] as? String) ?? ""
        guard let chatId = Int32(chatIdStr) else { return }

        let visitorName  = (data["visitor_name"]  as? String) ?? ""
        let visitorPhone = (data["visitor_phone"] as? String) ?? ""

        print("🚨 iOS SOS alert → chatId=\(chatId) name=\(visitorName) phone=\(visitorPhone)")
        SosNotificationHandler.shared.onSosAlertReceived(
            alertId:      chatId,
            visitorName:  visitorName,
            visitorPhone: visitorPhone
        )
    }

    // MARK: - Deep links

    func application(
        _ app: UIApplication,
        open url: URL,
        options: [UIApplication.OpenURLOptionsKey : Any] = [:]
    ) -> Bool {
        if PPPayment.checkDeeplink(url) {
            print("✅ PhonePe SDK handled deep link")
            return true
        }
        PaymentBridge.shared.handleDeepLink(url)
        return true
    }
}