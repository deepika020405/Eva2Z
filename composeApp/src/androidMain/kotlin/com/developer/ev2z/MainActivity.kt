package com.developer.ev2z

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.repository.AuthRepository
import com.developer.ev2z.data.local.StorageManagerImpl
import com.developer.ev2z.ui.screens.AuthPagerScreen
import com.developer.ev2z.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val apiService = ApiService()
        val repository = AuthRepository(apiService)
        val storage = StorageManagerImpl(this)
        val viewModel = AuthViewModel(repository, storage)

        setContent {
            MaterialTheme {
                AuthPagerScreen(
                    viewModel = viewModel,
                    onLoginSuccess = {
                        // Navigate to HomeActivity
                        // startActivity(Intent(this, HomeActivity::class.java))
                        // finish()
                    }
                )
            }
        }
    }
}