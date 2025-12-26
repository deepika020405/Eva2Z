package com.developer.ev2z

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.lifecycle.lifecycleScope
import com.developer.ev2z.data.local.initStorageManager
import com.developer.ev2z.phonepe.PaymentBus
import com.developer.ev2z.ui.screens.Profile.GlobalContext
import com.developer.ev2z.util.AppSignatureHelper
import com.developer.ev2z.utils.PlayIntegrityHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MainActivity", "🚀 App starting v4.9.5")

        try {
            // ✅ Initialize storage
            initStorageManager(this)

            // ✅ Setup activity
            ActivityProvider.setActivity(this)

            // ✅ Print signatures
            AppSignatureHelper(this).printAppSignaturesToLog()

            // ✅ Setup global context
            GlobalContext.set(this)

            // ✅ IMPORTANT: Verify Play Integrity
            lifecycleScope.launch {
                try {
                    val isLegit = PlayIntegrityHelper.verifyIntegrity(this@MainActivity)
                    Log.d("MainActivity", if (isLegit) "✅ App verified as legitimate" else "⚠️ Integrity check inconclusive")
                } catch (e: Exception) {
                    Log.w("MainActivity", "Integrity check skipped: ${e.message}")
                }
            }

            // ✅ Set UI content
            setContent {
                App()
            }

            Log.d("MainActivity", "✅ App initialized successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ Initialization error", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ActivityProvider.clearActivity()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        intent.data?.let { uri ->
            Log.d("PhonePe", "🔗 Payment callback: $uri")

            val status = uri.getQueryParameter("status")
            val txnId = uri.getQueryParameter("transactionId")
                ?: uri.getQueryParameter("providerReferenceId")
            val code = uri.getQueryParameter("code")

            when (status?.uppercase()) {
                "SUCCESS" -> {
                    Log.d("PhonePe", "✅ Payment SUCCESS")
                    PaymentBus.notify(true, txnId, "SUCCESS")
                }
                "FAILED" -> {
                    Log.d("PhonePe", "❌ Payment FAILED")
                    PaymentBus.notify(false, txnId, "FAILED")
                }
                "PENDING" -> {
                    Log.d("PhonePe", "⏳ Payment PENDING")
                    PaymentBus.notify(false, txnId, "PENDING")
                }
                else -> {
                    Log.d("PhonePe", "❓ Unknown: $status")
                    PaymentBus.notify(false, txnId, code ?: "UNKNOWN")
                }
            }
        }
    }
}

@Composable
fun AppAndroidPreview() {
    App()
}