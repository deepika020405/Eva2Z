// composeApp/src/commonMain/kotlin/com/developer/ev2z/App.kt
package com.developer.ev2z

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.developer.ev2z.data.local.createStorageManager
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.repository.AuthRepository
import com.developer.ev2z.ui.screens.AuthPagerScreen
import com.developer.ev2z.viewmodel.AuthViewModel

@Composable
fun App(onLoginSuccess: () -> Unit = {}) {
    // Common setup for both platforms
    val apiService = ApiService()
    val repository = AuthRepository(apiService)
    val storage = createStorageManager()
    val viewModel = AuthViewModel(repository, storage)

    MaterialTheme {
        AuthPagerScreen(
            viewModel = viewModel,
            onLoginSuccess = onLoginSuccess
        )
    }
}