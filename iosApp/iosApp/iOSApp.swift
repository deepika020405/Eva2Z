// iosApp/iOSApp.swift
import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    handlePaymentCallback(url)
                }
        }
    }
    
    private func handlePaymentCallback(_ url: URL) {
        print("📱 Deep Link: \(url.absoluteString)")
        
        // Check scheme
        guard url.scheme == "eva2zgps" else {
            print("⚠️ Unknown scheme")
            return
        }
        
        // Parse query parameters
        let components = URLComponents(url: url, resolvingAgainstBaseURL: false)
        let status = components?.queryItems?.first(where: { $0.name == "status" })?.value
        
        if status?.lowercased() == "success" {
            print("✅ Payment Success")
            PaymentBus.shared.notify(success: true, txnId: nil, reason: nil)
        } else {
            print("❌ Payment Failed/Cancelled")
            PaymentBus.shared.notify(success: false, txnId: nil, reason: status ?? "cancelled")
        }
    }
}