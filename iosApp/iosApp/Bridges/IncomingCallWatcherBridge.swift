import Foundation
import SocketIO
import ComposeApp

// Lightweight Socket.IO watcher for the ringing phase (before Accept).
// Joins the call room and listens for server broadcasts:
//   "call-resolved" { roomId, action="accepted" } — another device accepted
//   "call-ended"    { roomId }                    — caller cancelled / another device declined
//   "end-call"      { roomId }                    — backend alternative event name
//
// Mirrors Android's IncomingCallSocketWatcher exactly.
@objc class IncomingCallWatcherBridge: NSObject, IOSIncomingCallWatcherDelegate {

    @objc static let shared = IncomingCallWatcherBridge()

    private let signalingServerURL = "https://gps.eva2z.in"

    private var socketManager: SocketManager?
    private var socket: SocketIOClient?
    private var currentRoomId: String = ""

    // MARK: - IOSIncomingCallWatcherDelegate

    func startWatching(roomId: String) {
        stopWatching()
        currentRoomId = roomId
        print("🔌 [IncomingCallWatcher] Connecting for room=\(roomId)")

        guard let url = URL(string: signalingServerURL) else { return }

        let mgr = SocketManager(socketURL: url, config: [
            .log(false), .compress, .forceWebsockets(true), .reconnects(false)
        ])
        let s = mgr.defaultSocket
        socketManager = mgr
        socket = s

        s.on(clientEvent: .connect) { [weak self] _, _ in
            guard let self = self else { return }
            print("✅ [IncomingCallWatcher] Connected — joining room=\(self.currentRoomId)")
            s.emit("join-room", ["roomId": self.currentRoomId])
        }

        s.on(clientEvent: .error) { data, _ in
            print("❌ [IncomingCallWatcher] Connection error: \(data)")
        }

        // Another owner device accepted — dismiss IncomingCallScreen on this device
        s.on("call-resolved") { [weak self] data, _ in
            guard let self = self,
                  let dict      = data.first as? [String: Any],
                  let eventRoom = dict["roomId"] as? String,
                  let action    = dict["action"] as? String,
                  eventRoom == self.currentRoomId,
                  action == "accepted" else { return }
            print("📡 [IncomingCallWatcher] call-resolved accepted — dismissing IncomingCallScreen")
            DispatchQueue.main.async {
                RingtoneManager.shared.stopRinging()
                SosNotificationHandler.shared.onCallAcceptedElsewhere()
            }
        }

        // Caller cancelled or another device declined — dismiss on all devices
        s.on("call-ended") { [weak self] data, _ in
            guard let self = self,
                  let dict      = data.first as? [String: Any],
                  let eventRoom = dict["roomId"] as? String,
                  eventRoom == self.currentRoomId else { return }
            print("📡 [IncomingCallWatcher] call-ended — dismissing IncomingCallScreen")
            DispatchQueue.main.async {
                RingtoneManager.shared.stopRinging()
                SosNotificationHandler.shared.onCallEnded()
            }
        }

        s.on("end-call") { [weak self] data, _ in
            guard let self = self,
                  let dict      = data.first as? [String: Any],
                  let eventRoom = dict["roomId"] as? String,
                  eventRoom == self.currentRoomId else { return }
            print("📡 [IncomingCallWatcher] end-call — dismissing IncomingCallScreen")
            DispatchQueue.main.async {
                RingtoneManager.shared.stopRinging()
                SosNotificationHandler.shared.onCallEnded()
            }
        }

        s.connect()
    }

    func stopWatching() {
        guard socket != nil else { return }
        print("🔌 [IncomingCallWatcher] Disconnecting for room=\(currentRoomId)")
        socket?.off(clientEvent: .connect)
        socket?.off(clientEvent: .error)
        socket?.off("call-resolved")
        socket?.off("call-ended")
        socket?.off("end-call")
        socket?.disconnect()
        socket        = nil
        socketManager = nil
        currentRoomId = ""
    }
}