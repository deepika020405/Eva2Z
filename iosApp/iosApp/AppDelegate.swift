import UIKit
import AVFoundation
import CallKit
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

        // tokenFetcher lets FcmBridge.register() pull a fresh token after logout + re-login.
        // Without this, lastToken is nil after FcmBridge.clear() and no token ever arrives.
        FcmBridge.shared.tokenFetcher = {
            Messaging.messaging().token { token, error in
                if let error = error {
                    print("❌ FCM token refresh error: \(error.localizedDescription)")
                } else if let token = token {
                    FcmBridge.shared.onToken(token: token)
                }
            }
        }

        // Register WebRTC bridge
        WebRtcBridgeHolder.shared.delegate = WebRtcBridge.shared
        print("✅ WebRtcBridge registered")

        // Register CallKit bridge — lets Kotlin cancelCallNotification() dismiss native UI
        CallKitBridgeHolder.shared.delegate = CallKitManager.shared
        print("✅ CallKitBridge registered")

        // Register ringtone bridge — lets Kotlin stopCallSound() stop the foreground ringtone
        RingtoneBridgeHolder.shared.delegate = RingtoneManager.shared
        print("✅ RingtoneBridge registered")

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

    // MARK: - FCM message received (foreground + background)
    // Required when FirebaseAppDelegateProxyEnabled = NO.
    // Called for both data-only FCM and notification+data FCM (in background).

    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        Messaging.messaging().appDidReceiveMessage(userInfo)
        print("📨 didReceiveRemoteNotification:", userInfo)

        let type   = userInfo["type"]    as? String ?? ""
        let roomId = userInfo["room_id"] as? String ?? ""

        // Call-related FCM types — handle exclusively (no generic banner)
        if ["incoming_call", "call_ended", "call_accepted"].contains(type) {
            handleCallIfNeeded(data: userInfo)

            // Belt-and-suspenders: post a local notification with sound so the user
            // always hears something, even if CallKit takes a moment to appear.
            // Dismissed automatically when the call ends / is accepted.
            if type == "incoming_call" {
                let content = UNMutableNotificationContent()
                let callerName = userInfo["visitor_name"] as? String ?? "Visitor"
                content.title = callerName
                content.body  = "Incoming parking call — tap to answer"
                content.sound = UNNotificationSound.default
                if let dict = userInfo as? [String: Any] {
                    content.userInfo = dict
                }
                let req = UNNotificationRequest(
                    identifier: "incoming-call-\(roomId.isEmpty ? "bg" : roomId)",
                    content:    content,
                    trigger:    nil
                )
                UNUserNotificationCenter.current().add(req) { _ in }
            }

            completionHandler(.newData)
            return
        }

        // Generic SOS/chat — extract title/body, then decide whether to post a local banner.
        // For notification+data FCM: iOS auto-shows the banner in background; willPresent
        // shows it in foreground. We must NOT post a duplicate local notification.
        // For data-only FCM: no system notification — we must post the local banner ourselves.
        let aps   = userInfo["aps"] as? [AnyHashable: Any]
        let alert = aps?["alert"] as? [AnyHashable: Any]
        let title = (userInfo["title"] as? String)
                 ?? (alert?["title"] as? String)
                 ?? "New Alert"
        let body  = (userInfo["body"] as? String)
                 ?? (alert?["body"] as? String)
                 ?? ""

        let hasSystemNotification = alert != nil
        if !hasSystemNotification {
            // Data-only FCM — post a local banner (system won't show anything otherwise)
            let content = UNMutableNotificationContent()
            content.title = title
            content.body  = body
            content.sound = .default
            if let dict = userInfo as? [String: Any] {
                content.userInfo = dict
            }

            let chatKey = (userInfo["chat_id"] as? String) ?? UUID().uuidString
            let request = UNNotificationRequest(
                identifier: "chat-\(chatKey)",
                content:    content,
                trigger:    nil
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
        let identifier = notification.request.identifier
        let data = notification.request.content.userInfo
        let type = data["type"] as? String ?? ""
        print("📩 Foreground notification id=\(identifier) type=\(type)")

        // Suppress our background-fallback local notification when app is foreground
        // (IncomingCallScreen + RingtoneManager already handle the foreground call UI)
        if identifier.hasPrefix("incoming-call-") {
            center.removeDeliveredNotifications(withIdentifiers: [identifier])
            completionHandler([])
            return
        }

        if ["incoming_call", "call_ended", "call_accepted"].contains(type) {
            // Call events from FCM in foreground — Compose UI handles them.
            // Suppress the system banner; RingtoneManager plays a foreground ringtone.
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

            let chatId: KotlinInt? = chatIdInt > 0 ? KotlinInt(value: chatIdInt) : nil
            SosNotificationHandler.shared.onIncomingCallReceived(
                visitorName:  visitorName,
                visitorPhone: visitorPhone,
                roomId:       roomId,
                chatId:       chatId
            )

            let appState = UIApplication.shared.applicationState
            if appState == .background || appState == .inactive {
                // Background / locked screen — show CallKit native call UI (manages its own ringtone)
                CallKitManager.shared.reportIncomingCall(
                    visitorName:  visitorName,
                    visitorPhone: visitorPhone,
                    roomId:       roomId,
                    chatId:       chatIdInt
                )
            } else {
                // Foreground — Compose IncomingCallScreen is shown; play our own ringtone
                RingtoneManager.shared.startRinging()
            }

        case "call_ended":
            print("📵 iOS CALL ENDED")
            RingtoneManager.shared.stopRinging()
            dismissIncomingCallNotification()
            CallKitManager.shared.endCurrentCall(reason: .remoteEnded)
            SosNotificationHandler.shared.onCallEnded()

        case "call_accepted":
            print("📱 iOS CALL ACCEPTED ELSEWHERE")
            RingtoneManager.shared.stopRinging()
            dismissIncomingCallNotification()
            CallKitManager.shared.endCurrentCall(reason: .answeredElsewhere)
            SosNotificationHandler.shared.onCallAcceptedElsewhere()

        default:
            break
        }
    }

    // MARK: - Notification helpers

    private func dismissIncomingCallNotification() {
        let center = UNUserNotificationCenter.current()
        center.getDeliveredNotifications { notifications in
            let ids = notifications
                .map { $0.request.identifier }
                .filter { $0.hasPrefix("incoming-call-") }
            center.removeDeliveredNotifications(withIdentifiers: ids)
        }
        center.getPendingNotificationRequests { requests in
            let ids = requests
                .map { $0.identifier }
                .filter { $0.hasPrefix("incoming-call-") }
            center.removePendingNotificationRequests(withIdentifiers: ids)
        }
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