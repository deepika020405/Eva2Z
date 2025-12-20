// iosApp/Bridges/PaymentBridge.swift
import Foundation
import UIKit
import PhonePePayment
import ComposeApp

@objc class PaymentBridge: NSObject {

    static let shared = PaymentBridge()
    private var ppPayment: PPPayment?

    private override init() {
        super.init()
        setupObserver()
    }

    private func setupObserver() {
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleOpenPaymentNotification(_:)),
            name: NSNotification.Name("OpenPaymentWebView"),
            object: nil
        )
    }

    @objc private func handleOpenPaymentNotification(_ notification: Notification) {
        guard let urlString = notification.object as? String else {
            print("❌ Invalid notification payload")
            PaymentBus.shared.notify(success: false, txnId: nil, reason: "Invalid payment URL")
            return
        }

        // ✅ Extract token from URL
        guard let url = URL(string: urlString),
              let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
              let token = components.queryItems?.first(where: { $0.name == "token" })?.value else {
            print("❌ Failed to extract token")
            PaymentBus.shared.notify(success: false, txnId: nil, reason: "Invalid payment URL")
            return
        }

        print("📬 Token received: \(token.prefix(50))...")
        openPaymentWithSDK(token: token)
    }

    private func openPaymentWithSDK(token: String) {
        print("🚀 Initializing PhonePe SDK")

        // ✅ Initialize SDK
        ppPayment = PPPayment(
            environment: .production,
            flowId: "ABCDEFGHIJK12",
            merchantId: "M22VRDGEOVBYAUAT"
        )

        PPPayment.enableDebugLogs = true

        let orderId = "OMO\(Int(Date().timeIntervalSince1970 * 1000))"

        print("🔑 Order ID: \(orderId)")
        print("🔑 Token: \(token.prefix(50))...")

        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let rootViewController = windowScene.windows.first?.rootViewController else {
            print("❌ No root view controller")
            PaymentBus.shared.notify(success: false, txnId: nil, reason: "UI not available")
            return
        }

        // ✅ Start PhonePe checkout
        ppPayment?.startCheckoutFlow(
            merchantId: "M22VRDGEOVBYAUAT",
            orderId: orderId,
            token: token,
            appSchema: "eva2zgps",
            on: rootViewController
        ) { [weak self] transactionRequest, resultState in
            self?.handlePaymentResult(transactionRequest: transactionRequest, resultState: resultState)
        }

        print("✅ PhonePe SDK checkout started")
    }

    private func handlePaymentResult(transactionRequest: DPSTransactionRequest?, resultState: PPResultState) {
        print("📱 Payment Result State: \(resultState)")

        // ✅ Extract transaction ID if available
        let txnId = transactionRequest?.transactionId

        switch resultState {
        case .success:
            print("✅ Payment Success - txnId: \(txnId ?? "nil")")
            // ⭐ IMPORTANT: Don't mark success immediately
            // Backend will verify via webhook
            PaymentBus.shared.notify(success: true, txnId: txnId, reason: nil)

        case .failure(let error):
            print("❌ Payment Failed: \(error.localizedDescription)")
            PaymentBus.shared.notify(success: false, txnId: nil, reason: error.localizedDescription)

        case .interrupted(let error):
            print("🚫 Payment Interrupted/Cancelled: \(error.localizedDescription)")
            PaymentBus.shared.notify(success: false, txnId: nil, reason: "Payment cancelled by user")

        @unknown default:
            print("❓ Unknown payment state")
            PaymentBus.shared.notify(success: false, txnId: nil, reason: "Unknown payment state")
        }
    }

    func handleDeepLink(_ url: URL) {
        print("📱 Deep Link Received: \(url.absoluteString)")

        let handled = PPPayment.checkDeeplink(url)
        if handled {
            print("✅ PhonePe SDK handled deep link")
        } else {
            print("⚠️ Deep link not from PhonePe")
        }
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
    }
}