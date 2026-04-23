import CallKit
import AVFoundation
import Foundation
import ComposeApp

// Mirrors Android's IncomingCallActivity (full-screen lock-screen UI) +
// CallRingtoneService (looping ringtone) using the iOS-native CallKit framework.
//
// Flow (app backgrounded/suspended):
//   FCM "incoming_call" → AppDelegate.handleCallIfNeeded
//     → SosNotificationHandler.onIncomingCallReceived (stored in callFlow replay)
//     → CallKitManager.reportIncomingCall  ← native iOS call screen over lock screen
//   User answers via CallKit native UI
//     → CXAnswerCallAction → call /call/accept API
//     → SosNotificationHandler.onCallAcceptedByOwner → HomeNavGraph shows CallScreen
//   User declines via CallKit native UI
//     → CXEndCallAction → call /call/decline API → SosNotificationHandler.onCallEnded
//
// Flow (app in foreground):
//   FCM → onIncomingCallReceived → Compose shows IncomingCallScreen (normal flow)
//   IncomingCallScreen accept/decline → cancelCallNotification() in Kotlin
//     → CallKitBridgeHolder.dismissFromApp() → endCurrentCall(reason:) here

// IOSCallKitDelegate is the Kotlin interface from CallKitBridgeHolder.kt.
// Conformance lets Kotlin's cancelCallNotification() call dismissFromApp() here.
class CallKitManager: NSObject, CXProviderDelegate, IOSCallKitDelegate {

    static let shared = CallKitManager()

    private(set) var provider: CXProvider
    let callController = CXCallController()

    // UUID of the currently reported call (nil when no call is active)
    private(set) var currentCallUUID: UUID?

    // Pending call metadata — set on reportIncomingCall, consumed on answer/decline
    private var pendingRoomId:       String = ""
    private var pendingVisitorName:  String = ""
    private var pendingVisitorPhone: String = ""
    private var pendingChatId:       Int32  = -1

    private override init() {
        let config = CXProviderConfiguration()
        config.supportsVideo            = false
        config.maximumCallsPerCallGroup = 1
        config.maximumCallGroups        = 1
        config.supportedHandleTypes     = [.phoneNumber, .generic]

        provider = CXProvider(configuration: config)
        super.init()
        provider.setDelegate(self, queue: .main)
    }

    // MARK: - Report incoming call to system (shows native call screen)

    func reportIncomingCall(
        visitorName:  String,
        visitorPhone: String,
        roomId:       String,
        chatId:       Int32
    ) {
        // Dismiss any stale call first
        if let existing = currentCallUUID {
            provider.reportCall(with: existing, endedAt: Date(), reason: .remoteEnded)
            currentCallUUID = nil
        }

        pendingRoomId       = roomId
        pendingVisitorName  = visitorName
        pendingVisitorPhone = visitorPhone
        pendingChatId       = chatId

        let uuid = UUID()
        currentCallUUID = uuid

        let update = CXCallUpdate()
        let handle = visitorPhone.isEmpty ? visitorName : visitorPhone
        update.remoteHandle         = CXHandle(type: .phoneNumber,
                                               value: handle.isEmpty ? "Unknown" : handle)
        update.localizedCallerName  = visitorName.isEmpty ? "Visitor" : visitorName
        update.hasVideo             = false
        update.supportsHolding      = false
        update.supportsGrouping     = false
        update.supportsUngrouping   = false
        update.supportsDTMF         = false

        provider.reportNewIncomingCall(with: uuid, update: update) { error in
            if let error = error {
                print("❌ [CallKit] reportNewIncomingCall error: \(error.localizedDescription)")
                self.currentCallUUID = nil
            } else {
                print("✅ [CallKit] Incoming call reported to system uuid=\(uuid)")
            }
        }
    }

    // MARK: - Dismiss from code (remote ended / accepted elsewhere / declined in app)

    func endCurrentCall(reason: CXCallEndedReason = .remoteEnded) {
        guard let uuid = currentCallUUID else { return }
        print("📵 [CallKit] Ending call uuid=\(uuid) reason=\(reason.rawValue)")
        provider.reportCall(with: uuid, endedAt: Date(), reason: reason)
        currentCallUUID = nil
    }

    // Called via CallKitBridgeHolder when Kotlin cancelCallNotification() fires
    // (user accepted or declined from the Compose IncomingCallScreen)
    func dismissFromApp() {
        endCurrentCall(reason: .answeredElsewhere)
    }

    // MARK: - CXProviderDelegate

    func providerDidReset(_ provider: CXProvider) {
        print("🔄 [CallKit] Provider reset")
        currentCallUUID = nil
    }

    func provider(_ provider: CXProvider, perform action: CXAnswerCallAction) {
        print("✅ [CallKit] User answered via native UI — room=\(pendingRoomId)")
        action.fulfill()

        let roomId       = pendingRoomId
        let visitorName  = pendingVisitorName
        let visitorPhone = pendingVisitorPhone
        let chatId       = pendingChatId
        currentCallUUID  = nil

        // Auth token stored by Kotlin IOSStorageManager → UserDefaults key "token"
        let token = UserDefaults.standard.string(forKey: "token") ?? ""

        callAcceptAPI(token: token, roomId: roomId) {
            DispatchQueue.main.async {
                // Emit callAcceptedByOwnerFlow so HomeNavGraph skips IncomingCallScreen
                // and goes directly to CallScreen (mirrors Android IncomingCallActivity flow)
                let chatIdKt: KotlinInt? = chatId > 0 ? KotlinInt(value: chatId) : nil
                SosNotificationHandler.shared.onCallAcceptedByOwner(
                    visitorName:  visitorName,
                    visitorPhone: visitorPhone,
                    roomId:       roomId,
                    chatId:       chatIdKt
                )
            }
        }
    }

    func provider(_ provider: CXProvider, perform action: CXEndCallAction) {
        print("📵 [CallKit] User declined via native UI — room=\(pendingRoomId)")
        action.fulfill()

        let roomId = pendingRoomId
        let token  = UserDefaults.standard.string(forKey: "token") ?? ""
        currentCallUUID = nil

        // Tell Kotlin the call ended so Compose dismisses any related UI
        SosNotificationHandler.shared.onCallEnded()

        // Fire-and-forget decline API
        callDeclineAPI(token: token, roomId: roomId, completion: nil)
    }

    // CallKit activates the audio session when the user answers.
    // Let WebRtcBridge reconfigure its audio after the system session is active.
    func provider(_ provider: CXProvider, didActivate audioSession: AVAudioSession) {
        print("🔊 [CallKit] Audio session activated by system")
        WebRtcBridge.shared.onCallKitAudioActivated()
    }

    func provider(_ provider: CXProvider, didDeactivate audioSession: AVAudioSession) {
        print("🔇 [CallKit] Audio session deactivated")
    }

    // MARK: - Private REST helpers

    private func callAcceptAPI(token: String, roomId: String, completion: (() -> Void)?) {
        guard !token.isEmpty, !roomId.isEmpty,
              let url = URL(string: "https://gps.eva2z.in/api/call/accept") else {
            print("⚠️ [CallKit] acceptCall skipped — token or roomId empty")
            completion?()
            return
        }
        var req = URLRequest(url: url)
        req.httpMethod = "POST"
        req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        req.setValue("application/json",  forHTTPHeaderField: "Content-Type")
        req.httpBody = try? JSONSerialization.data(withJSONObject: ["room_id": roomId])

        URLSession.shared.dataTask(with: req) { _, _, err in
            if let err = err { print("❌ [CallKit] acceptCall API error: \(err)") }
            else             { print("✅ [CallKit] acceptCall API success") }
            completion?()
        }.resume()
    }

    private func callDeclineAPI(token: String, roomId: String, completion: (() -> Void)?) {
        guard !token.isEmpty, !roomId.isEmpty,
              let url = URL(string: "https://gps.eva2z.in/api/call/decline") else {
            completion?()
            return
        }
        var req = URLRequest(url: url)
        req.httpMethod = "POST"
        req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        req.setValue("application/json",  forHTTPHeaderField: "Content-Type")
        req.httpBody = try? JSONSerialization.data(withJSONObject: ["room_id": roomId])

        URLSession.shared.dataTask(with: req) { _, _, err in
            if let err = err { print("❌ [CallKit] declineCall API error: \(err)") }
            else             { print("✅ [CallKit] declineCall API success") }
            completion?()
        }.resume()
    }
}