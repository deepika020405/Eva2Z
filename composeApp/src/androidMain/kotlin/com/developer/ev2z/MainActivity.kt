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
import com.phonepe.intent.sdk.api.PhonePe
import com.phonepe.intent.sdk.api.models.PhonePeEnvironment

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initStorageManager(this)

        // Initialize PhonePe SDK here (one time)
        try {
            PhonePe.init(
                this,
                PhonePeEnvironment.RELEASE, // Use SANDBOX for testing
                "M22VRDGEOVBYAUAT", // Your merchant ID
                null
            )
            Log.d("PhonePe", "SDK initialized successfully")
        } catch (e: Exception) {
            Log.e("PhonePe", "SDK init failed: ${e.message}")
        }

        ActivityProvider.setActivity(this)

        setContent {
            App()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // ✅ Memory leak avoid karne ke liye clear karo
        ActivityProvider.clearActivity()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PHONEPE_REQUEST_CODE) {
            Log.d("PhonePe", "Payment result: resultCode=$resultCode")

            when (resultCode) {
                Activity.RESULT_OK -> {
                    val transactionId = data?.getStringExtra("transactionId")
                    Log.d("PhonePe", "Payment SUCCESS: txnId=$transactionId")
                    PaymentBus.onResult(true, transactionId, "SUCCESS")
                }
                Activity.RESULT_CANCELED -> {
                    Log.d("PhonePe", "Payment CANCELLED")
                    PaymentBus.onResult(false, null, "CANCELLED")
                }
                else -> {
                    val error = data?.getStringExtra("error") ?: "Unknown error"
                    Log.d("PhonePe", "Payment FAILED: $error")
                    PaymentBus.onResult(false, null, error)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        intent.data?.let { uri ->
            Log.d("PhonePe", "Deep link callback: $uri")

            val status = uri.getQueryParameter("status")
            val txnId = uri.getQueryParameter("transactionId")

            PaymentBus.onResult(
                success = status == "SUCCESS",
                txnId = txnId,
                reason = status
            )
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