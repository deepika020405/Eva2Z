package com.developer.ev2z

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import com.developer.ev2z.data.local.initStorageManager
import com.developer.ev2z.ui.screens.Profile.GlobalContext
import com.developer.ev2z.util.AppSignatureHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize storage for Android
        initStorageManager(this)

        val signatureHelper = AppSignatureHelper(this)
        signatureHelper.printAppSignaturesToLog()

        // Initialize GlobalContext for URL opening
        GlobalContext.set(this)

        setContent {
            App()
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
