// App.kt
package com.developer.ev2z

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.data.local.UserPrefs
import com.developer.ev2z.data.local.createStorageManager
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.repository.AuthRepository
import com.developer.ev2z.ui.screens.AuthPagerScreen
import com.developer.ev2z.ui.screens.homeScreen.HomeViewModel
import com.developer.ev2z.viewmodel.AuthViewModel
import com.developer.ev2z.navigation.HomeNavGraph
import com.developer.ev2z.repository.HomeRepository
import kotlinx.coroutines.delay

@Composable
fun App() {
    // Create dependencies only once
    val apiService = remember { ApiService() }
    val authRepository = remember { AuthRepository(apiService) }
    val homeRepository = remember { HomeRepository(apiService) }
    val storage = remember { createStorageManager() }
    val userPrefs = remember { UserPrefs(storage) }
    val authViewModel = remember { AuthViewModel(authRepository, storage) }
    val homeViewModel = remember { HomeViewModel(homeRepository) }

    var isLoading by remember { mutableStateOf(true) }
    var isLoggedIn by remember { mutableStateOf(false) }
    var authToken by remember { mutableStateOf("") }
    val loginSuccess by authViewModel.loginSuccess.collectAsState()

    // Splash Screen Logic
    LaunchedEffect(Unit) {
        delay(2000) // 2 seconds splash delay

        // Check if user is already logged in
        isLoggedIn = userPrefs.isLoggedIn() == true

        // Get saved token if exists
        if (isLoggedIn) {
            authToken = userPrefs.getToken() ?: ""
            println("📱 Token loaded from storage: $authToken")
        }

        isLoading = false
    }

    // Listen for login success
    LaunchedEffect(loginSuccess) {
        if (loginSuccess) {
            // Get token from UserPrefs after successful login
            authToken = userPrefs.getToken() ?: ""
            isLoggedIn = authToken.isNotEmpty()
            println("✅ Login successful! Token: $authToken")
        }
    }

    MaterialTheme {
        when {
            // Show Splash Screen
            isLoading -> {
                SplashScreen()
            }

            // Show Home Screen if logged in
            isLoggedIn && authToken.isNotEmpty() -> {
                HomeNavGraph(
                    viewModel = homeViewModel,
                    token = authToken,
                    onLogout = {
                        println("🚪 Logging out...")
                        userPrefs.clear()
                        isLoggedIn = false
                        authToken = ""
                        authViewModel.resetLoginSuccess()
                    }
                )
            }

            // Show Auth Screen (Login/Signup)
            else -> {
                AuthPagerScreen(
                    viewModel = authViewModel,
                    onLoginSuccess = {
                        println("🎉 onLoginSuccess triggered")
                        authViewModel.resetLoginSuccess()
                        authViewModel.setError("")
                        authViewModel.setLoginSuccess(true)
                    }
                )
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF4CAF50)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "EVA2Z",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Connect",
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(40.dp),
                strokeWidth = 4.dp
            )
        }
    }
}



//package com.developer.ev2z
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import com.developer.ev2z.data.local.UserPrefs
//import com.developer.ev2z.data.local.createStorageManager
//import com.developer.ev2z.network.ApiService
//import com.developer.ev2z.repository.AuthRepository
//import com.developer.ev2z.ui.screens.AuthPagerScreen
//import com.developer.ev2z.viewmodel.AuthViewModel
//import com.developer.ev2z.navigation.HomeNavGraph
//import kotlinx.coroutines.delay
//
//@Composable
//fun App() {
//    // Create dependencies only once
//    val apiService = remember { ApiService() }
//    val repository = remember { AuthRepository(apiService) }
//    val storage = remember { createStorageManager() }
//    val userPrefs = remember { UserPrefs(storage) }
//    val viewModel = remember { AuthViewModel(repository, storage) }
//
//    var isLoading by remember { mutableStateOf(true) }
//    var isLoggedIn by remember { mutableStateOf(false) }
//    val loginSuccess by viewModel.loginSuccess.collectAsState()
//
//    // Splash Screen Logic
//    LaunchedEffect(Unit) {
//        delay(2000) // 2 seconds splash delay
//
//        // Check if user is already logged in
//        isLoggedIn = userPrefs.isLoggedIn()
//        isLoading = false
//    }
//
//    MaterialTheme {
//        when {
//            // Show Splash Screen
//            isLoading -> {
//                SplashScreen()
//            }
//
//            // Show Home Screen if logged in or login successful
//            isLoggedIn || loginSuccess -> {
//                HomeNavGraph(
//                    onLogout = {
//                        userPrefs.clear()
//                        isLoggedIn = false
//                        viewModel.resetLoginSuccess()
//                    }
//                )
//            }
//
//            // Show Auth Screen (Login/Signup)
//            else -> {
//                AuthPagerScreen(
//                    viewModel = viewModel,
//                    onLoginSuccess = {
//                        viewModel.resetLoginSuccess()
//                        viewModel.setError("")
//                        viewModel.setLoginSuccess(true)
//                        isLoggedIn = true
//                    }
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun SplashScreen() {
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(Color(0xFF4CAF50)),
//        contentAlignment = Alignment.Center
//    ) {
//        Column(
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.Center
//        ) {
//            // App Logo/Name
//            Text(
//                text = "EVA2Z",
//                fontSize = 48.sp,
//                fontWeight = FontWeight.Bold,
//                color = Color.White
//            )
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            Text(
//                text = "Connect",
//                fontSize = 20.sp,
//                fontWeight = FontWeight.Normal,
//                color = Color.White.copy(alpha = 0.9f)
//            )
//
//            Spacer(modifier = Modifier.height(32.dp))
//
//            CircularProgressIndicator(
//                color = Color.White,
//                modifier = Modifier.size(40.dp),
//                strokeWidth = 4.dp
//            )
//        }
//    }
//}