// iosApp/Bridges/PaymentBridge.swift
import Foundation
import UIKit
import WebKit
import PhonePePayment
import ComposeApp

@objc class PaymentBridge: NSObject {

    static let shared = PaymentBridge()
    private var ppPayment: PPPayment?

    private override init() {
        super.init()

        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleOpenPaymentNotification(_:)),
            name: NSNotification.Name("OpenPaymentWebView"),
            object: nil
        )
    }

    @objc private func handleOpenPaymentNotification(_ notification: Notification) {
        guard let info = notification.userInfo,
              let token = info["token"] as? String,
              let orderId = info["orderId"] as? String,
              !token.isEmpty, !orderId.isEmpty else {
            print("❌ Invalid payment notification payload")
            PaymentBus.shared.notify(success: false, txnId: nil, reason: "Invalid payment request")
            return
        }

        print("📬 Received payment request — orderId: \(orderId), token: \(token.prefix(50))...")
        openPaymentWithSDK(token: token, orderId: orderId)
    }

    private func openPaymentWithSDK(token: String, orderId: String) {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }

            // Before the PPPayment init — the SDK reads this when it builds its logger,
            // so setting it afterwards silenced the first (and most interesting) events.
            PPPayment.enableDebugLogs = true

            self.ppPayment = PPPayment(
                environment: .production,
                flowId: "ABCDEFGHIJK12",
                merchantId: "M22VRDGEOVBYAUAT",
                enableLogging: true
            )

            guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                  let rootViewController = windowScene.windows.first?.rootViewController else {
                PaymentBus.shared.notify(success: false, txnId: nil, reason: "UI not available")
                return
            }

            // Compose puts its own content on the root VC and can have a sheet up, so the
            // root is not always what is on screen. Presenting onto a VC that already has
            // a presentedViewController fails silently and the checkout never appears.
            var presenter = rootViewController
            while let presented = presenter.presentedViewController {
                presenter = presented
            }

            self.ppPayment?.startCheckoutFlow(
                merchantId: "M22VRDGEOVBYAUAT",
                orderId: orderId,
                token: token,
                appSchema: "eva2zgps",
                on: presenter
            ) { [weak self] request, resultState in
                self?.handlePaymentResult(request: request, resultState: resultState)
            }

            #if DEBUG
            // The SDK owns its WKWebView, and "Something went wrong" is drawn by the page
            // inside it — the SDK never reports a terminal state for it, so no amount of
            // callback logging can reach the cause. Marking the web view inspectable lets
            // Safari > Develop > <device> attach and show the real console/network error.
            self.enableWebViewInspection(from: presenter)
            #endif
        }
    }

    #if DEBUG
    /// Polls for the checkout WKWebView the SDK presents and marks it inspectable.
    /// Presentation and page load are both async, so a single pass usually finds nothing.
    private func enableWebViewInspection(from presenter: UIViewController, attempt: Int = 0) {
        guard attempt < 20 else {
            print("🔎 Gave up looking for the checkout web view — cannot enable inspection")
            return
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in
            var top = presenter
            while let presented = top.presentedViewController {
                top = presented
            }

            if let webView = Self.findWebView(in: top.view) {
                if #available(iOS 16.4, *) {
                    webView.isInspectable = true
                    print("🔎 Checkout web view is now inspectable — attach via Safari > Develop")
                } else {
                    print("🔎 Found checkout web view, but inspection needs iOS 16.4+")
                }
                return
            }

            self?.enableWebViewInspection(from: presenter, attempt: attempt + 1)
        }
    }

    private static func findWebView(in view: UIView) -> WKWebView? {
        if let webView = view as? WKWebView { return webView }
        for subview in view.subviews {
            if let found = findWebView(in: subview) { return found }
        }
        return nil
    }
    #endif

    private func handlePaymentResult(request: DPSTransactionRequest?, resultState: PPResultState) {
        // `code` is the only field that distinguishes a merchant-config rejection from a
        // declined instrument — localizedDescription is "Something went wrong" for both.
        print("📱 Payment Result State: \(resultState)")
        if let request = request {
            print("🧾 Transaction request: \(request)")
        }

        switch resultState {
        case .success:
            print("✅ Payment Success")
            PaymentBus.shared.notify(success: true, txnId: nil, reason: nil)

        case .failure(let error):
            print("❌ Payment Failed — code: \(error.code), reason: \(error.localizedDescription)")
            PaymentBus.shared.notify(
                success: false,
                txnId: nil,
                reason: "\(error.localizedDescription) (\(error.code))"
            )

        case .interrupted(let error):
            print("🚫 Payment Interrupted — code: \(error.code), reason: \(error.localizedDescription)")
            PaymentBus.shared.notify(success: false, txnId: nil, reason: "Payment cancelled")

        @unknown default:
            print("❓ Unknown state")
            PaymentBus.shared.notify(success: false, txnId: nil, reason: "Unknown error")
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
