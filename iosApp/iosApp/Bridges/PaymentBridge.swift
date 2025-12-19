// iosApp/Bridges/PaymentBridge.swift
import Foundation
import UIKit
import SafariServices
import ComposeApp

@objc class PaymentBridge: NSObject {

    static let shared = PaymentBridge()
    private var safariViewController: SFSafariViewController?

    private override init() {
        super.init()

        // ✅ Listen for payment open requests from Kotlin
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleOpenPaymentNotification(_:)),
            name: NSNotification.Name("OpenPaymentWebView"),
            object: nil
        )
    }

    // ✅ Handle notification from Kotlin
    @objc private func handleOpenPaymentNotification(_ notification: Notification) {
        guard let urlString = notification.object as? String else {
            print("❌ Invalid notification payload")
            return
        }

        print("📬 Received payment open request: \(urlString)")
        openPaymentInApp(urlString)
    }

    // ✅ Open payment in IN-APP WebView
    func openPaymentInApp(_ urlString: String) {
        guard let url = URL(string: urlString) else {
            print("❌ Invalid URL: \(urlString)")
            PaymentBus.shared.notify(
                success: false,
                txnId: nil,
                reason: "Invalid payment URL"
            )
            return
        }

        print("🚀 Opening In-App Payment WebView")

        // ✅ Ensure we're on main thread
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }

            // Get root view controller
            guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                  let rootViewController = windowScene.windows.first?.rootViewController else {
                print("❌ Failed to get root view controller")
                PaymentBus.shared.notify(
                    success: false,
                    txnId: nil,
                    reason: "UI not available"
                )
                return
            }

            // ✅ Create SFSafariViewController (In-App Browser)
            let safariVC = SFSafariViewController(url: url)
            safariVC.delegate = self

            // Customize appearance
            safariVC.preferredBarTintColor = UIColor(red: 92/255, green: 36/255, blue: 138/255, alpha: 1.0)
            safariVC.preferredControlTintColor = .white
            safariVC.modalPresentationStyle = .fullScreen

            self.safariViewController = safariVC

            // ✅ Present the in-app browser
            rootViewController.present(safariVC, animated: true) {
                print("✅ In-App WebView opened")
            }
        }
    }

    // ✅ Close payment WebView
    func closePaymentWebView() {
        DispatchQueue.main.async { [weak self] in
            self?.safariViewController?.dismiss(animated: true) {
                print("🔒 Payment WebView closed")
                self?.safariViewController = nil
            }
        }
    }

    // ✅ Handle deep link callback
    func handleDeepLink(_ url: URL) {
        print("📱 Deep Link Received: \(url.absoluteString)")

        // Close WebView first
        closePaymentWebView()

        guard url.scheme == "eva2zgps" else {
            print("⚠️ Unknown scheme: \(url.scheme ?? "nil")")
            return
        }

        guard let components = URLComponents(url: url, resolvingAgainstBaseURL: false) else {
            print("❌ Failed to parse URL components")
            PaymentBus.shared.notify(
                success: false,
                txnId: nil,
                reason: "Invalid callback URL"
            )
            return
        }

        let status = components.queryItems?.first(where: { $0.name == "status" })?.value
        let transactionId = components.queryItems?.first(where: { $0.name == "transactionId" })?.value
        let code = components.queryItems?.first(where: { $0.name == "code" })?.value

        print("📊 Status: \(status ?? "unknown"), Code: \(code ?? "unknown")")

        // ✅ Notify based on status
        if status?.lowercased() == "success" || code == "PAYMENT_SUCCESS" {
            print("✅ Payment Success")
            PaymentBus.shared.notify(success: true, txnId: transactionId, reason: nil)
        } else {
            print("❌ Payment Failed: \(status ?? code ?? "cancelled")")
            PaymentBus.shared.notify(success: false, txnId: nil, reason: status ?? code ?? "cancelled")
        }
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
    }
}

// ✅ Delegate - Handle user dismissal
extension PaymentBridge: SFSafariViewControllerDelegate {
    func safariViewControllerDidFinish(_ controller: SFSafariViewController) {
        print("⚠️ User dismissed payment WebView")
        PaymentBus.shared.notify(success: false, txnId: nil, reason: "Payment cancelled by user")
        safariViewController = nil
    }
}