import Foundation
import SocketIO
import ComposeApp

// Mirrors Android's ChatSocketManager exactly.
//
// Server URL : https://gps.eva2z.in  (same as Android)
// Emits      : joinChat, user_online, user_offline, send_message, close_chat, leave_chat
// Receives   : newMessage, message_status, user_online, user_offline, chat_closed

@objc class ChatSocketBridge: NSObject, IOSChatSocketDelegate {

    @objc static let shared = ChatSocketBridge()

    private var socketManager: SocketManager?
    private var socket: SocketIOClient?

    // Stored as Int (SocketData-compatible); Kotlin passes Int32 at the protocol boundary.
    private var activeChatId: Int = 0
    private var activeUserId: Int = 0

    // MARK: - IOSChatSocketDelegate

    func connect(serverUrl: String, chatId: Int32, userId: Int32, token: String) {
        disconnect()

        activeChatId = Int(chatId)
        activeUserId = Int(userId)

        guard let url = URL(string: serverUrl) else {
            print("❌ [ChatSocketBridge] Invalid URL: \(serverUrl)"); return
        }

        // Mirror Android's socket.io-client options exactly:
        // - forceNew     : always create a fresh socket (no stale-connection reuse)
        // - forceWebsockets: prefer WebSocket transport (same as Android's transports=["websocket","polling"])
        // - reconnectWaitMax: cap backoff at 10 s (Android's reconnectionDelayMax=10000)
        socketManager = SocketManager(socketURL: url, config: [
            .log(false),
            .compress,
            .reconnects(true),
            .reconnectAttempts(5),
            .reconnectWait(2),
            .reconnectWaitMax(10),
            .forceNew(true),
            .forceWebsockets(true)
        ])
        socket = socketManager?.defaultSocket

        socket?.on(clientEvent: .connect) { [weak self] _, _ in
            guard let self = self else { return }
            print("✅ [ChatSocketBridge] Connected — joining chatId=\(self.activeChatId)")
            // joinChat: send chatId as plain integer (matches Android emit("joinChat", chatId))
            self.socket?.emit("joinChat", self.activeChatId)
            self.emitUserOnlineInt(chatId: self.activeChatId, userId: self.activeUserId)
            ChatSocketBridgeHolder.shared.callback?.onConnected()
        }

        socket?.on(clientEvent: .error) { data, _ in
            print("❌ [ChatSocketBridge] Socket error: \(data)")
            ChatSocketBridgeHolder.shared.callback?.onDisconnected()
        }

        socket?.on(clientEvent: .disconnect) { data, _ in
            print("🔌 [ChatSocketBridge] Disconnected: \(data)")
            ChatSocketBridgeHolder.shared.callback?.onDisconnected()
        }

        socket?.on("newMessage") { data, _ in
            print("📨 [ChatSocketBridge] newMessage raw: \(data)")
            guard let dict = data.first as? [String: Any] else {
                print("❌ [ChatSocketBridge] newMessage: could not parse payload — \(data)")
                return
            }
            let msgObj = (dict["message"] as? [String: Any]) ?? dict

            // Parse id robustly: SocketIO-Swift returns NSNumber from JSON,
            // which bridges to Int in Swift. Fall back through NSNumber and Double
            // to prevent silent id=0 which causes messages to be dropped downstream.
            let id: Int32
            if let n = msgObj["id"] as? NSNumber {
                id = n.int32Value
            } else if let i = msgObj["id"] as? Int {
                id = Int32(i)
            } else if let d = msgObj["id"] as? Double {
                id = Int32(d)
            } else {
                id = 0
                print("⚠️ [ChatSocketBridge] newMessage: could not parse id from \(String(describing: msgObj["id"]))")
            }

            let message   = (msgObj["text"]      as? String).flatMap { $0.isEmpty ? nil : $0 }
                         ?? (msgObj["message"]   as? String).flatMap { $0.isEmpty ? nil : $0 }
            let type      = (msgObj["type"]      as? String).flatMap { $0.isEmpty ? nil : $0 }
            let time      = (msgObj["time"]      as? String).flatMap { $0.isEmpty ? nil : $0 }
            let timestamp = (msgObj["timestamp"] as? String).flatMap { $0.isEmpty ? nil : $0 }

            print("📨 [ChatSocketBridge] newMessage id=\(id) type=\(type ?? "") text=\(message?.prefix(40) ?? "")")
            ChatSocketBridgeHolder.shared.callback?.onNewMessage(
                id: id, message: message, type: type, time: time, timestamp: timestamp
            )
        }

        socket?.on("message_status") { data, _ in
            guard let dict = data.first as? [String: Any] else { return }
            let msgId: Int32
            if let n = dict["message_id"] as? NSNumber { msgId = n.int32Value }
            else if let i = dict["message_id"] as? Int { msgId = Int32(i) }
            else { return }
            let isDelivered = (dict["is_delivered"] as? NSNumber)?.intValue == 1
                           || (dict["is_delivered"] as? Int) == 1
            let isSeen      = (dict["is_seen"] as? NSNumber)?.intValue == 1
                           || (dict["is_seen"] as? Int) == 1
            guard msgId > 0 else { return }
            print("📬 [ChatSocketBridge] message_status id=\(msgId) delivered=\(isDelivered) seen=\(isSeen)")
            ChatSocketBridgeHolder.shared.callback?.onMessageStatus(
                messageId: msgId, isDelivered: isDelivered, isSeen: isSeen
            )
        }

        socket?.on("user_online") { data, _ in
            guard let dict = data.first as? [String: Any] else { return }
            let eventUserId: Int32
            if let n = dict["user_id"] as? NSNumber { eventUserId = n.int32Value }
            else if let i = dict["user_id"] as? Int { eventUserId = Int32(i) }
            else { eventUserId = -1 }
            print("🟢 [ChatSocketBridge] user_online eventUserId=\(eventUserId)")
            ChatSocketBridgeHolder.shared.callback?.onVisitorOnline(eventUserId: eventUserId)
        }

        socket?.on("user_offline") { data, _ in
            guard let dict = data.first as? [String: Any] else { return }
            let eventUserId: Int32
            if let n = dict["user_id"] as? NSNumber { eventUserId = n.int32Value }
            else if let i = dict["user_id"] as? Int { eventUserId = Int32(i) }
            else { eventUserId = -1 }
            print("🔴 [ChatSocketBridge] user_offline eventUserId=\(eventUserId)")
            ChatSocketBridgeHolder.shared.callback?.onVisitorOffline(eventUserId: eventUserId)
        }

        socket?.on("chat_closed") { _, _ in
            print("🔒 [ChatSocketBridge] chat_closed")
            ChatSocketBridgeHolder.shared.callback?.onChatClosed()
        }

        // connect(withPayload:) sends the token as a Socket.IO v3 auth object —
        // equivalent to Android's auth = mapOf("token" to token).
        print("📡 [ChatSocketBridge] Connecting to \(serverUrl) — chatId=\(activeChatId) userId=\(activeUserId)")
        socket?.connect(withPayload: ["token": token])
    }

    func emitUserOnline(chatId: Int32, userId: Int32) {
        emitUserOnlineInt(chatId: Int(chatId), userId: Int(userId))
    }

    func emitUserOffline(chatId: Int32, userId: Int32) {
        // Use plain [String: Any] — SocketIO-Swift serialises NSDictionary as JSON object
        socket?.emit("user_offline", ["chat_id": Int(chatId), "user_id": Int(userId), "role": "owner"])
        print("🔴 [ChatSocketBridge] user_offline — chatId=\(chatId) userId=\(userId)")
    }

    func emitOwnerMessage(chatId: Int32, text: String) {
        // Use plain [String: Any] so SocketIO serialises it as a JSON object,
        // matching Android's JSONObject payload for the send_message event.
        socket?.emit("send_message", ["chat_id": Int(chatId), "message": text])
        print("📤 [ChatSocketBridge] send_message — chatId=\(chatId) text=\(text.prefix(40))")
    }

    func emitCloseChat(chatId: Int32) {
        socket?.emit("close_chat", ["chat_id": Int(chatId)])
        print("📤 [ChatSocketBridge] close_chat — chatId=\(chatId)")
    }

    func emitMessagesSeen(chatId: Int32) {
        socket?.emit("messages_seen", ["chat_id": Int(chatId)])
        print("👁️ [ChatSocketBridge] messages_seen — chatId=\(chatId)")
    }

    func disconnect() {
        if let s = socket {
            s.emit("leave_chat", ["chat_id": activeChatId])
            s.removeAllHandlers()
            s.disconnect()
        }
        socket        = nil
        socketManager = nil
        activeChatId  = 0
        activeUserId  = 0
        print("🔌 [ChatSocketBridge] Socket cleaned up")
    }

    // MARK: - Private

    private func emitUserOnlineInt(chatId: Int, userId: Int) {
        socket?.emit("user_online", ["chat_id": chatId, "user_id": userId, "role": "owner"])
        print("🟢 [ChatSocketBridge] user_online — chatId=\(chatId) userId=\(userId)")
    }
}