package com.developer.ev2z

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import com.developer.ev2z.data.local.initStorageManager
import com.developer.ev2z.phonepe.PHONEPE_REQUEST_CODE
import com.developer.ev2z.phonepe.PaymentBus
import com.developer.ev2z.ui.screens.Profile.GlobalContext
import com.developer.ev2z.util.AppSignatureHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initStorageManager(this)

        // ✅ Activity ko set karo
        ActivityProvider.setActivity(this)
        val signatureHelper = AppSignatureHelper(this)
        signatureHelper.printAppSignaturesToLog()

        // Initialize GlobalContext for URL opening
        GlobalContext.set(this)

        setContent {
            App()
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
            Log.d("PhonePe", "🔗 Callback URI: $uri")

            // Extract payment info from URL
            // Example: ev2z://payment/callback?status=SUCCESS&transactionId=TX123
            val status = uri.getQueryParameter("status")
            val txnId = uri.getQueryParameter("transactionId")
                ?: uri.getQueryParameter("providerReferenceId")
            val code = uri.getQueryParameter("code")

            Log.d("PhonePe", "Status: $status, TxnId: $txnId, Code: $code")

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
                    Log.d("PhonePe", "❓ Unknown status: $status")
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

//package com.developer.ev2z
//
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.compose.material3.MaterialTheme
//import com.developer.ev2z.network.ApiService
//import com.developer.ev2z.repository.AuthRepository
//import com.developer.ev2z.data.local.initStorageManager
//import com.developer.ev2z.data.local.createStorageManager
//import com.developer.ev2z.ui.screens.AuthPagerScreen
//import com.developer.ev2z.viewmodel.AuthViewModel
//
//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        // Initialize storage manager with context (no return value)
//        initStorageManager(this)
//
//        // Create instances
//        val apiService = ApiService()
//        val repository = AuthRepository(apiService)
//        val storage = createStorageManager()  // ✅ Yeh StorageManager return karta hai
//        val viewModel = AuthViewModel(repository, storage)
//
//        setContent {
//            MaterialTheme {
//                AuthPagerScreen(
//                    viewModel = viewModel,
//                    onLoginSuccess = {
//                        // Navigate to HomeActivity
//                        // startActivity(Intent(this, HomeActivity::class.java))
//                        // finish()
//                    }
//                )
//            }
//        }
//    }
//}

