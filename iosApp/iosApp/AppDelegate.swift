import UIKit
import AVFoundation
import CallKit
import PushKit
import ComposeApp
import PhonePePayment
import FirebaseCore
import FirebaseMessaging
import UserNotifications

class AppDelegate: NSObject,
                   UIApplicationDelegate,
                   UNUserNotificationCenterDelegate,
                   MessagingDelegate,
                   PKPushRegistryDelegate {

    private var voipRegistry: PKPushRegistry?

    // Prevents double-processing when both VoIP push and FCM push arrive for the same call.
    // Whichever path (VoIP or FCM) processes first wins; the other is silently ignored.
    private var lastHandledRoomId: String = ""

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

        // VoIP PushKit — wakes app immediately on any state (background / killed / locked)
        // so CallKit can ring before the user ever taps a notification.
        let voipReg = PKPushRegistry(queue: .main)
        voipReg.delegate = self
        voipReg.desiredPushTypes = [.voIP]
        self.voipRegistry = voipReg
        print("✅ VoIP PushKit registered")

        // Cold-start: app killed and user tapped a notification banner
        if let payload = launchOptions?[UIApplication.LaunchOptionsKey.remoteNotification] as? [AnyHashable: Any] {
            print("🧊 Cold-start notification payload:", payload)
            handleCallIfNeeded(data: payload, isUserTap: true)
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
        // Re-register VoIP token now that an auth token is likely available.
        if let voipToken = UserDefaults.standard.string(forKey: "voip_push_token") {
            registerVoipToken(voipToken)
        }
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

            // Belt-and-suspenders: only post a local notification for data-only FCM messages
            // (no aps.alert). When the server sends a notification+data message (aps.alert present),
            // iOS already shows the banner — posting another would create a duplicate.
            // When the server adds content-available:1, aps.alert is still present, so we skip this.
            if type == "incoming_call" {
                let aps           = userInfo["aps"] as? [AnyHashable: Any]
                let hasSystemAlert = aps?["alert"] != nil

                if !hasSystemAlert {
                    let content    = UNMutableNotificationContent()
                    let callerName = userInfo["visitor_name"] as? String ?? "Visitor"
                    content.title  = callerName
                    content.body   = "Incoming parking call — tap to answer"
                    if #available(iOS 15.2, *) {
                        content.sound = UNNotificationSound.defaultRingtone
                    } else {
                        content.sound = UNNotificationSound.default
                    }
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

        if type == "incoming_call" {
            let tappedRoomId = data["room_id"] as? String ?? ""
            if !tappedRoomId.isEmpty && tappedRoomId == lastHandledRoomId {
                // VoIP/FCM push already stored Kotlin call state and posted the notification.
                // App is coming to foreground — dismiss the notification banner and ring.
                print("📲 Notification tapped — call state ready, opening IncomingCallScreen")
                dismissIncomingCallNotification()
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                    RingtoneManager.shared.startRinging()
                }
            } else {
                // Cold start — no prior push processing. Build state from scratch.
                handleCallIfNeeded(data: data, isUserTap: true)
            }
        } else if ["call_ended", "call_accepted"].contains(type) {
            handleCallIfNeeded(data: data)
        } else {
            triggerSosIfNeeded(data: data)
        }

        completionHandler()
    }

    // MARK: - Call handling

    private func handleCallIfNeeded(data: [AnyHashable: Any], isUserTap: Bool = false) {
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
            guard roomId.isEmpty || roomId != lastHandledRoomId else {
                print("⚠️ incoming_call ignored — duplicate roomId already being handled: \(roomId)")
                return
            }
            lastHandledRoomId = roomId
            print("📞 iOS INCOMING CALL — visitor=\(visitorName) phone=\(visitorPhone) room=\(roomId)")

            let chatId: KotlinInt? = chatIdInt > 0 ? KotlinInt(value: chatIdInt) : nil
            let appState = UIApplication.shared.applicationState

            // Always store call state in Kotlin so IncomingCallScreen is ready whenever
            // the app becomes visible (foreground, opened from tap, or cold start).
            SosNotificationHandler.shared.onIncomingCallReceived(
                visitorName:  visitorName,
                visitorPhone: visitorPhone,
                roomId:       roomId,
                chatId:       chatId
            )

            if isUserTap {
                // User tapped from cold start (no prior push processing):
                // IncomingCallScreen is visible, start ringtone after app finishes loading.
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                    RingtoneManager.shared.startRinging()
                }
            } else if appState == .active {
                // Foreground FCM: IncomingCallScreen already visible — just ring.
                RingtoneManager.shared.startRinging()
            } else {
                // Background/locked FCM (VoIP push not available or not yet arrived):
                // ring via background audio + post a silent banner on the lock screen.
                RingtoneManager.shared.startRinging()
                postIncomingCallNotification(
                    visitorName:  visitorName,
                    visitorPhone: visitorPhone,
                    roomId:       roomId,
                    originalData: data
                )
            }

        case "call_ended":
            print("📵 iOS CALL ENDED")
            lastHandledRoomId = ""
            RingtoneManager.shared.stopRinging()
            dismissIncomingCallNotification()
            CallKitManager.shared.endCurrentCall(reason: .remoteEnded)
            SosNotificationHandler.shared.onCallEnded()

        case "call_accepted":
            print("📱 iOS CALL ACCEPTED ELSEWHERE")
            lastHandledRoomId = ""
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

    // MARK: - Incoming call notification (lock screen banner + ringtone, no CallKit UI)

    private func postIncomingCallNotification(
        visitorName:  String,
        visitorPhone: String,
        roomId:       String,
        originalData: [AnyHashable: Any]
    ) {
        let content       = UNMutableNotificationContent()
        content.title     = visitorName.isEmpty ? "Incoming Call" : visitorName
        content.body      = visitorPhone.isEmpty ? "Tap to answer" : "\(visitorPhone) — tap to answer"
        // No notification sound — RingtoneManager plays continuous audio in the background.
        // A one-shot notification sound would overlap or cut off the looping ringtone.
        content.sound     = nil

        if let dict = originalData as? [String: Any] {
            content.userInfo = dict
        }

        let identifier = "incoming-call-\(roomId.isEmpty ? UUID().uuidString : roomId)"
        let request    = UNNotificationRequest(identifier: identifier, content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request) { err in
            if let err = err { print("❌ [Notification] incoming-call post failed:", err) }
            else             { print("🔔 [Notification] incoming-call posted id=\(identifier)") }
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

    // MARK: - VoIP PushKit

    func pushRegistry(
        _ registry: PKPushRegistry,
        didUpdate pushCredentials: PKPushCredentials,
        for type: PKPushType
    ) {
        guard type == .voIP else { return }
        let token = pushCredentials.token.map { String(format: "%02x", $0) }.joined()
        print("📱 VoIP push token:", token)
        // Persist so re-registration can happen after login if auth token isn't ready yet.
        UserDefaults.standard.set(token, forKey: "voip_push_token")
        registerVoipToken(token)
    }

    // iOS calls this the instant a VoIP push arrives — even when the app is killed.
    // Apple REQUIRES reportNewIncomingCall to be called before completion(); if we
    // skip it, iOS terminates the app and may restrict future VoIP push delivery.
    func pushRegistry(
        _ registry: PKPushRegistry,
        didReceiveIncomingPushWith payload: PKPushPayload,
        for type: PKPushType,
        completion: @escaping () -> Void
    ) {
        guard type == .voIP else { completion(); return }
        let data = payload.dictionaryPayload
        print("🔔 VoIP push received:", data)

        let visitorName  = data["visitor_name"]  as? String ?? ""
        let visitorPhone = data["visitor_phone"] as? String ?? ""
        let roomId       = data["room_id"]       as? String ?? ""
        let chatIdStr    = data["chat_id"]       as? String ?? ""
        let chatIdInt    = Int32(chatIdStr) ?? -1
        let chatId: KotlinInt? = chatIdInt > 0 ? KotlinInt(value: chatIdInt) : nil

        guard !WebRtcBridge.shared.isCallActive else {
            // Already in a call — must still satisfy Apple's reportNewIncomingCall
            // requirement; report then immediately end so no UI flickers.
            let uuid   = UUID()
            let update = CXCallUpdate()
            update.remoteHandle = CXHandle(type: .generic, value: "busy")
            CallKitManager.shared.provider.reportNewIncomingCall(with: uuid, update: update) { _ in
                CallKitManager.shared.provider.reportCall(with: uuid, endedAt: Date(), reason: .failed)
                completion()
            }
            return
        }

        // Deduplication: FCM push may arrive for the same roomId shortly after VoIP push.
        // If this VoIP push is first, claim the roomId; if FCM was first, skip.
        if !roomId.isEmpty && roomId == lastHandledRoomId {
            print("⚠️ VoIP push ignored — duplicate roomId already being handled: \(roomId)")
            // Must still report to satisfy Apple's requirement, then end immediately.
            let uuid   = UUID()
            let update = CXCallUpdate()
            update.remoteHandle = CXHandle(type: .generic, value: "duplicate")
            CallKitManager.shared.provider.reportNewIncomingCall(with: uuid, update: update) { _ in
                CallKitManager.shared.provider.reportCall(with: uuid, endedAt: Date(), reason: .failed)
                completion()
            }
            return
        }
        lastHandledRoomId = roomId

        // Store call state in Kotlin (needed if user opens the app and Compose
        // UI appears — it will find the call waiting in the flow).
        SosNotificationHandler.shared.onIncomingCallReceived(
            visitorName:  visitorName,
            visitorPhone: visitorPhone,
            roomId:       roomId,
            chatId:       chatId
        )

        // Ring in every state (foreground, background, locked, killed).
        // With `audio` in UIBackgroundModes, AVAudioPlayer keeps playing while the app
        // is in the background — the user hears continuous ringing on the lock screen.
        RingtoneManager.shared.startRinging()

        let appState = UIApplication.shared.applicationState
        if appState != .active {
            // Post a silent notification so a banner appears on the lock screen.
            // Sound comes from RingtoneManager (continuous), not the notification (one-shot).
            postIncomingCallNotification(
                visitorName:  visitorName,
                visitorPhone: visitorPhone,
                roomId:       roomId,
                originalData: [
                    "type":          "incoming_call",
                    "room_id":       roomId,
                    "visitor_name":  visitorName,
                    "visitor_phone": visitorPhone,
                    "chat_id":       chatIdStr
                ]
            )
        }

        // Apple REQUIRES reportNewIncomingCall before completion() on VoIP push.
        // We immediately end it so no CallKit full-screen UI appears — our notification
        // banner is the only thing the user sees on the lock screen.
        let uuid   = UUID()
        let update = CXCallUpdate()
        let handle = visitorPhone.isEmpty ? visitorName : visitorPhone
        update.remoteHandle        = CXHandle(type: .phoneNumber, value: handle.isEmpty ? "Unknown" : handle)
        update.localizedCallerName = visitorName.isEmpty ? "Visitor" : visitorName
        update.hasVideo            = false
        CallKitManager.shared.provider.reportNewIncomingCall(with: uuid, update: update) { _ in
            CallKitManager.shared.provider.reportCall(with: uuid, endedAt: Date(), reason: .failed)
            completion()
        }
    }

    func pushRegistry(
        _ registry: PKPushRegistry,
        didInvalidatePushTokenFor type: PKPushType
    ) {
        print("⚠️ VoIP push token invalidated for type: \(type.rawValue)")
    }

    private func registerVoipToken(_ voipToken: String) {
        let authToken = UserDefaults.standard.string(forKey: "token") ?? ""
        guard !authToken.isEmpty, !voipToken.isEmpty,
              let url = URL(string: "https://gps.eva2z.in/api/fcm/register") else { return }

        let deviceUUID = UIDevice.current.identifierForVendor?.uuidString ?? ""
        var req = URLRequest(url: url)
        req.httpMethod = "POST"
        req.setValue("Bearer \(authToken)", forHTTPHeaderField: "Authorization")
        req.setValue("application/json",    forHTTPHeaderField: "Content-Type")
        req.httpBody = try? JSONSerialization.data(withJSONObject: [
            "fcm_token":   voipToken,
            "device_uuid": deviceUUID,
            "platform":    "ios_voip"
        ])

        URLSession.shared.dataTask(with: req) { _, resp, err in
            if let err = err {
                print("❌ VoIP token registration error:", err)
            } else {
                let code = (resp as? HTTPURLResponse)?.statusCode ?? 0
                print("✅ VoIP token registered (HTTP \(code))")
            }
        }.resume()
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