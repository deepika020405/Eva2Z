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
import com.developer.ev2z.ui.screens.Auth.AuthPagerScreen
import com.developer.ev2z.ui.screens.ChangePassword.UpdatePasswordViewModel
import com.developer.ev2z.ui.screens.homeScreen.HomeViewModel
import com.developer.ev2z.viewmodel.AuthViewModel
import com.developer.ev2z.navigation.HomeNavGraph
import com.developer.ev2z.repository.DeviceReportRepository
import com.developer.ev2z.repository.HomeRepository
import com.developer.ev2z.ui.screens.AddDevice.AddDeviceViewModel
import com.developer.ev2z.ui.screens.AddDevice.AddDeviceResult
import com.developer.ev2z.ui.screens.AddDevice.BottomSheet
import com.developer.ev2z.ui.screens.AddDeviceSuccess.AddDeviceSuccessScreen
import com.developer.ev2z.ui.screens.ChangePassword.ChangePasswordScreen
import com.developer.ev2z.ui.screens.device_Report.DeviceReportScreen
import com.developer.ev2z.ui.screens.device_Report.DeviceReportViewModel
import com.developer.ev2z.ui.screens.device_Report.PlatformAddressProvider
import com.developer.ev2z.ui.screens.AuthFlows.ForgetPasswordFlow
import com.developer.ev2z.ui.screens.AuthFlows.RegistrationFlow
import kotlinx.coroutines.delay

@Composable
fun App() {
    val apiService = remember { ApiService() }
    val authRepository = remember { AuthRepository(apiService) }
    val homeRepository = remember { HomeRepository(apiService) }
    val deviceReportRepository = remember { DeviceReportRepository(apiService) }
    val storage = remember { createStorageManager() }
    val userPrefs = remember { UserPrefs(storage) }
    val authViewModel = remember { AuthViewModel(authRepository, storage) }
    val homeViewModel = remember { HomeViewModel(homeRepository) }
    val addDeviceViewModel = remember { AddDeviceViewModel() }

    // ✅ NEW: UpdatePasswordViewModel
    val updatePasswordViewModel = remember { UpdatePasswordViewModel(apiService, storage) }

    val platformContext = getPlatformContext()
    val addressProvider = remember { PlatformAddressProvider(platformContext) }

    val deviceReportViewModel = remember {
        DeviceReportViewModel(
            repository = deviceReportRepository,
            platformAddressProvider = addressProvider
        )
    }

    var isLoading by remember { mutableStateOf(true) }
    var isLoggedIn by remember { mutableStateOf(false) }
    var authToken by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var showAddDevice by remember { mutableStateOf(false) }
    var showDeviceReport by remember { mutableStateOf(false) }
    var showAddDeviceSuccess by remember { mutableStateOf(false) }

    // Authentication flow states
    var showForgetPassword by remember { mutableStateOf(false) }
    var showRegistration by remember { mutableStateOf(false) }
    var registrationName by remember { mutableStateOf("") }
    var registrationPhone by remember { mutableStateOf("") }
    var registrationEmail by remember { mutableStateOf("") }

    // ✅ NEW: Change Password state
    var showChangePassword by remember { mutableStateOf(false) }

    // Success screen data
    var successDeviceName by remember { mutableStateOf("") }
    var successDeviceId by remember { mutableStateOf("") }
    var successSimNumber by remember { mutableStateOf("") }
    var successDeviceType by remember { mutableStateOf("") }

    val loginSuccess by authViewModel.loginSuccess.collectAsState()
    val addDeviceResult by addDeviceViewModel.addDeviceResult.collectAsState()
    var forgetPasswordPhone by remember { mutableStateOf("") }

    // Initial load
    LaunchedEffect(Unit) {
        println("🚀 App: Starting initial load")
        delay(2000)
        isLoggedIn = userPrefs.isLoggedIn() == true
        if (isLoggedIn) {
            authToken = userPrefs.getToken() ?: ""
            username = userPrefs.getName() ?: "User"
            println("📱 Token loaded from storage: $authToken")
        }
        isLoading = false
    }

    // Login success handler
    LaunchedEffect(loginSuccess) {
        if (loginSuccess) {
            authToken = userPrefs.getToken() ?: ""
            isLoggedIn = authToken.isNotEmpty()
            println("✅ Login successful! Token: $authToken")
        }
    }

    // AddDevice result handler
    LaunchedEffect(addDeviceResult) {
        when (val result = addDeviceResult) {
            is AddDeviceResult.Success -> {
                println("✅ Device added successfully, showing success screen")
                successDeviceName = result.deviceName
                successDeviceId = result.deviceId
                successSimNumber = result.simNumber
                successDeviceType = result.deviceType
                showAddDevice = false
                showAddDeviceSuccess = true
            }
            is AddDeviceResult.Error -> {
                println("❌ Device add error: ${result.message}")
            }
            null -> { /* No result */ }
        }
    }

    MaterialTheme {
        when {
            isLoading -> {
                SplashScreen()
            }

            // UPDATE showChangePassword rendering
            showChangePassword -> {
                println("📱 Rendering: Change Password Screen")
                ChangePasswordScreen(
                    apiService = apiService,
                    storageManager = storage,
                    onBackPressed = {
                        println("🔙 Back from Change Password")
                        showChangePassword = false
                    }
                )
            }

            // Forget Password Flow
            showForgetPassword -> {
                println("📱 Rendering: Forget Password Flow")
                ForgetPasswordFlow(
                    authViewModel = authViewModel,
                    preFilledPhone = forgetPasswordPhone,
                    onBack = {
                        println("🔙 Back to Auth from Forget Password")
                        showForgetPassword = false
                        forgetPasswordPhone = ""
                    },
                    onSuccess = {
                        println("✅ Password reset successful")
                        showForgetPassword = false
                        forgetPasswordPhone = ""
                    }
                )
            }

            // Registration Flow
            showRegistration -> {
                println("📱 Rendering: Registration Flow")
                RegistrationFlow(
                    authViewModel = authViewModel,
                    name = registrationName,
                    phone = registrationPhone,
                    email = registrationEmail,
                    onBack = {
                        println("🔙 Back to Auth from Registration")
                        showRegistration = false
                    },
                    onSuccess = {
                        println("✅ Registration successful")
                        showRegistration = false
                    }
                )
            }

            showAddDeviceSuccess -> {
                println("📱 Rendering: Success Screen")
                AddDeviceSuccessScreen(
                    onNavigate = { route ->
                        when (route) {
                            "home" -> {
                                println("🏠 Navigating back to home from success")
                                showAddDeviceSuccess = false
                                addDeviceViewModel.clearData()
                                homeViewModel.loadDevices(authToken)
                            }
                        }
                    },
                    deviceName = successDeviceName,
                    deviceId = successDeviceId,
                    simNumber = successSimNumber,
                    deviceType = successDeviceType
                )
            }

            showAddDevice -> {
                println("📱 Rendering: Add Device Screen")
                BottomSheet(
                    onNavigate = { route ->
                        when (route) {
                            "home" -> {
                                println("🏠 Navigating back to home from add device")
                                showAddDevice = false
                                addDeviceViewModel.clearData()
                            }
                            "addDeviceDeteails" -> {
                                println("📝 Staying on add device details")
                            }
                        }
                    },
                    viewModel = addDeviceViewModel
                )
            }

            showDeviceReport -> {
                println("📱 Rendering: Device Report Screen")
                DeviceReportScreen(
                    viewModel = deviceReportViewModel,
                    token = authToken,
                    username = username,
                    onBack = {
                        println("🔙 Back from Device Report to Home")
                        showDeviceReport = false
                    }
                )
            }

            isLoggedIn && authToken.isNotEmpty() -> {
                println("📱 Rendering: Home Screen")
                HomeNavGraph(
                    viewModel = homeViewModel,
                    apiService = apiService,
                    storageManager = storage,
                    token = authToken,
                    onNavigateToAddDevice = {
                        println("➕ Navigate to Add Device triggered")
                        showAddDevice = true
                    },
                    onNavigateToDeviceReport = {
                        println("➕ Navigate to Device Report triggered")
                        showDeviceReport = true
                    },
                    onNavigateToChangePassword = { // ✅ NEW
                        println("🔑 Navigate to Change Password triggered")
                        showChangePassword = true
                    },
                    onLogout = {
                        println("🚪 Logging out...")
                        userPrefs.clear()
                        isLoggedIn = false
                        authToken = ""
                        authViewModel.resetLoginSuccess()
                    }
                )
            }

            else -> {
                println("📱 Rendering: Auth Screen")
                AuthPagerScreen(
                    viewModel = authViewModel,
                    onLoginSuccess = {
                        println("🎉 onLoginSuccess triggered")
                        authViewModel.resetLoginSuccess()
                        authViewModel.clearError()
                        authViewModel.setLoginSuccess(true)
                    },
                    onForgetPasswordClick = { phone ->
                        println("🔑 Forget Password clicked with phone: $phone")
                        forgetPasswordPhone = phone
                        showForgetPassword = true
                    },
                    onSignupOTPClick = { name, phone, email ->
                        println("📧 Signup OTP clicked: $name, $phone, $email")
                        registrationName = name
                        registrationPhone = phone
                        registrationEmail = email
                        val fullPhone = "+91$phone"
                        authViewModel.signUp(name, fullPhone, "", email)
                        showRegistration = true
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