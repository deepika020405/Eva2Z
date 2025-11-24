package com.developer.ev2z

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import com.developer.ev2z.data.local.createStorageManager
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.repository.AuthRepository
import com.developer.ev2z.ui.screens.AuthPagerScreen
import com.developer.ev2z.viewmodel.AuthViewModel
import com.developer.ev2z.navigation.HomeNavGraph

@Composable
fun App() {

    // Create dependencies only once
    val apiService = remember { ApiService() }
    val repository = remember { AuthRepository(apiService) }
    val storage = remember { createStorageManager() }
    val viewModel = remember { AuthViewModel(repository, storage) }

    val loginSuccess by viewModel.loginSuccess.collectAsState()

    MaterialTheme {

        if (loginSuccess) {

            // HOME SCREEN
            HomeNavGraph(
                onLogout = {
                    viewModel.resetLoginSuccess()
                }
            )

        } else {

            // AUTH SCREEN (Login + Signup)
            AuthPagerScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    // Just flip the state
                    viewModel.resetLoginSuccess()
                    viewModel.setError("")
                    viewModel.setLoginSuccess(true)   // ensure navigation
                }
            )
        }
    }
}