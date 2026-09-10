// iosApp/Bridges/PaymentBridge.swift
import Foundation
import UIKit
import WebKit
import PhonePePayment
import ComposeApp

@objc class PaymentBridge: NSObject {

    static let shared = PaymentBridge()
    private var ppPayment: PPPayment?

    #if DEBUG
    private static let consoleHandlerName = "ev2zCheckoutDebug"
    private var didAttachConsoleBridge = false
    #endif

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

            #if DEBUG
            // Each attempt gets a fresh web view, so the bridge has to be re-attached.
            self.didAttachConsoleBridge = false
            #endif

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
                self?.attachConsoleBridge(to: webView)
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

    /// Mirrors the checkout page's console errors and network responses into the Xcode
    /// console. Safari's Web Inspector shows the same thing, but it needs the device to be
    /// paired and the page to still be on screen when you go looking — this survives both.
    private func attachConsoleBridge(to webView: WKWebView) {
        guard !didAttachConsoleBridge else { return }
        didAttachConsoleBridge = true

        let controller = webView.configuration.userContentController
        // Adding a duplicate name is a hard crash, and the SDK may hand back a controller
        // a previous attempt already touched.
        controller.removeScriptMessageHandler(forName: Self.consoleHandlerName)
        controller.add(self, name: Self.consoleHandlerName)

        // The user script covers every navigation the checkout makes from here on…
        controller.addUserScript(
            WKUserScript(
                source: Self.consoleBridgeSource,
                injectionTime: .atDocumentStart,
                forMainFrameOnly: false
            )
        )

        // …and this covers the document already on screen, which the user script missed.
        webView.evaluateJavaScript(Self.consoleBridgeSource) { _, error in
            if let error = error {
                print("🔎 Console bridge injection failed: \(error.localizedDescription)")
            } else {
                print("🔎 Console bridge attached — checkout errors will print here")
            }
        }
    }

    private static let consoleBridgeSource = """
    (function () {
      if (window.__ev2zDebugAttached) { return; }
      window.__ev2zDebugAttached = true;

      var post = function (payload) {
        try { window.webkit.messageHandlers.\(consoleHandlerName).postMessage(payload); } catch (e) {}
      };
      var clip = function (value) { return String(value == null ? '' : value).slice(0, 2000); };

      ['error', 'warn'].forEach(function (level) {
        var original = console[level];
        console[level] = function () {
          post({ kind: 'console', level: level, text: Array.prototype.map.call(arguments, String).join(' ') });
          original.apply(console, arguments);
        };
      });

      window.addEventListener('error', function (e) {
        post({ kind: 'jsError', text: e.message + ' @ ' + e.filename + ':' + e.lineno });
      });
      window.addEventListener('unhandledrejection', function (e) {
        post({ kind: 'jsError', text: 'unhandled rejection: ' + String(e.reason) });
      });

      var originalFetch = window.fetch;
      if (originalFetch) {
        window.fetch = function (input) {
          var url = String(input && input.url ? input.url : input);
          return originalFetch.apply(this, arguments).then(function (res) {
            res.clone().text().then(function (body) {
              post({ kind: 'fetch', status: res.status, url: url, body: clip(body) });
            }).catch(function () {});
            return res;
          }).catch(function (err) {
            post({ kind: 'fetch', status: -1, url: url, body: clip(err) });
            throw err;
          });
        };
      }

      var open = XMLHttpRequest.prototype.open;
      var send = XMLHttpRequest.prototype.send;
      XMLHttpRequest.prototype.open = function (method, url) {
        this.__ev2zUrl = url;
        return open.apply(this, arguments);
      };
      XMLHttpRequest.prototype.send = function () {
        var xhr = this;
        xhr.addEventListener('loadend', function () {
          post({ kind: 'xhr', status: xhr.status, url: String(xhr.__ev2zUrl), body: clip(xhr.responseText) });
        });
        return send.apply(this, arguments);
      };
    })();
    """
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

#if DEBUG
extension PaymentBridge: WKScriptMessageHandler {

    func userContentController(
        _ userContentController: WKUserContentController,
        didReceive message: WKScriptMessage
    ) {
        guard message.name == Self.consoleHandlerName,
              let info = message.body as? [String: Any] else { return }

        let kind = info["kind"] as? String ?? "?"

        switch kind {
        case "console", "jsError":
            print("🌐 [checkout \(kind)] \(info["text"] as? String ?? "")")

        default:
            let status = info["status"] as? Int ?? 0
            let url = info["url"] as? String ?? ""
            print("🌐 [checkout \(kind) \(status)] \(url)")
            if let body = info["body"] as? String, !body.isEmpty {
                print("   ↳ \(body)")
            }
        }
    }
}
#endif
